package com.BeSpoke.service;

import com.BeSpoke.exception.BadRequestException;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Uploaded images. Writes to a Google Cloud Storage bucket when {@code app.storage.bucket}
 * is set, and to ./uploads otherwise so local development needs no cloud credentials.
 *
 * <p>Either way the returned URL is <strong>absolute</strong>. It used to be the relative
 * path "/uploads/x.png", which the browser resolves against whatever origin the page came
 * from — so every image stored through the CRM 404'd on bespokedesign.in, because the file
 * only ever existed on the API host. A stored URL has to be complete on its own: it is
 * written into the database and read back by a different site.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5MB

    /** Object name prefix inside the bucket, so uploads don't sit at its root. */
    private static final String PREFIX = "uploads/";

    /** UUID filenames never change contents, so they can be cached forever. */
    private static final String CACHE_FOREVER = "public, max-age=31536000, immutable";

    private final Path uploadDir = Paths.get("uploads");
    private final String bucket;
    private final String publicBaseUrl;

    /**
     * Null when no bucket is configured, or when the startup probe could not reach it.
     * Not final: {@link #reportStorageState()} clears it if the bucket turns out to be
     * unreachable. A configured bucket never silently falls back to local disk.
     */
    private volatile Storage gcs;
    private String gcsError;

    public FileStorageService(
            @Value("${app.storage.bucket:}") String bucket,
            @Value("${app.storage.public-base-url:http://localhost:8080}") String publicBaseUrl) {
        this.bucket = bucket == null ? "" : bucket.trim();
        this.publicBaseUrl = stripTrailingSlash(publicBaseUrl);

        Storage client = null;
        String error = null;
        if (!this.bucket.isEmpty()) {
            try {
                // Application Default Credentials: the GOOGLE_APPLICATION_CREDENTIALS env
                // var pointing at a service-account JSON, or the instance's own service
                // account when running on GCP. Nothing to configure here either way.
                client = StorageOptions.getDefaultInstance().getService();
            } catch (Exception ex) {
                // Deliberately not fatal. A broken bucket config must not stop the whole
                // backend booting — but it must be impossible to miss, hence the ERROR
                // here and the startup report below.
                error = ex.toString();
            }
        }
        this.gcs = client;
        this.gcsError = error;
    }

    /**
     * Says out loud, once, where uploads are actually going — and proves it rather than
     * assuming it. Building the Storage client resolves credentials lazily: it succeeds
     * even when there are none, and the failure only surfaces on the first real upload.
     * So this makes one cheap call (read a key that will not exist) and reports what
     * actually happened. Without the probe this log line would cheerfully say "enabled"
     * while every upload silently fell over.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void reportStorageState() {
        if (bucket.isEmpty()) {
            log.warn("[UPLOADS] app.storage.bucket is not set — images are being written to"
                    + " ./uploads on this machine and served from {}. Fine for development;"
                    + " in production they disappear on the next deploy. Set STORAGE_BUCKET"
                    + " to a GCS bucket name.", publicBaseUrl);
            return;
        }
        if (gcs != null) {
            try {
                // Reading a missing object needs only storage.objects.get — the permission
                // an upload needs. Deliberately not buckets.get: the Storage Object Admin
                // role does not grant it, so probing the bucket itself would fail for a
                // correctly configured service account.
                gcs.get(BlobId.of(bucket, PREFIX + ".access-probe"));
            } catch (Exception ex) {
                gcs = null;
                gcsError = ex.toString();
            }
        }
        if (gcs == null) {
            throw new IllegalStateException("Configured storage is unavailable; check bucket permissions and credentials. Local fallback is disabled.");
        } else {
            log.info("[UPLOADS] enabled — writing to gs://{}/{}", bucket, PREFIX);
        }
    }

    /** Validates and stores an image; returns the absolute URL it is now readable at. */
    public String storeImage(MultipartFile file) {
        if (!bucket.isEmpty() && gcs == null) {
            throw new IllegalStateException("Configured storage is unavailable");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Only image uploads are allowed");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("File exceeds the 5MB size limit");
        }
        String filename = UUID.randomUUID() + extensionOf(file.getOriginalFilename());
        return gcs != null ? storeInBucket(file, filename, contentType) : storeOnDisk(file, filename);
    }

    /** Only self-contained GLB 2.0 assets; arbitrary uploaded HTML and external resources are rejected. */
    public String storeModel(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > 20L * 1024 * 1024)
            throw new BadRequestException("Choose a GLB model up to 20MB");
        if (!bucket.isEmpty() && gcs == null) throw new IllegalStateException("Configured storage is unavailable");
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length < 24) throw new IllegalArgumentException();
            var data = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            if (data.getInt() != 0x46546c67 || data.getInt() != 2 || data.getInt() != bytes.length)
                throw new IllegalArgumentException();
            int jsonLength = data.getInt();
            if (data.getInt() != 0x4e4f534a || jsonLength < 4 || jsonLength % 4 != 0 || jsonLength > data.remaining())
                throw new IllegalArgumentException();
            byte[] json = new byte[jsonLength]; data.get(json);
            var root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
            if (root == null || !"2.0".equals(root.path("asset").path("version").asText())) throw new IllegalArgumentException();
            if (!root.path("meshes").isArray() || root.path("meshes").isEmpty()) throw new IllegalArgumentException();
            for (String array : java.util.List.of("buffers", "images")) {
                for (var resource : root.path(array)) {
                    String uri = resource.path("uri").asText("");
                    if (!uri.isEmpty() && !uri.startsWith("data:")) throw new IllegalArgumentException();
                }
            }
            if (data.hasRemaining()) {
                if (data.remaining() < 8) throw new IllegalArgumentException();
                int binLength = data.getInt();
                if (data.getInt() != 0x004e4942 || binLength < 0 || binLength % 4 != 0 || binLength != data.remaining())
                    throw new IllegalArgumentException();
            }
        } catch (IOException | IllegalArgumentException ex) {
            throw new BadRequestException("Invalid model. Export a self-contained GLB 2.0 file with embedded textures and geometry");
        }
        String name = UUID.randomUUID() + ".glb";
        return gcs != null ? storeInBucket(file, name, "model/gltf-binary") : storeOnDisk(file, name);
    }

    private String storeInBucket(MultipartFile file, String filename, String contentType) {
        String object = PREFIX + filename;
        BlobInfo blob = BlobInfo.newBuilder(BlobId.of(bucket, object))
                .setContentType(contentType)
                .setCacheControl(CACHE_FOREVER)
                .build();
        try (var in = file.getInputStream()) {
            // No per-object ACL is set: buckets with uniform bucket-level access — the
            // default for new buckets — reject them outright. Public read comes from
            // granting allUsers objectViewer on the bucket instead (see README).
            gcs.createFrom(blob, in);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to upload to gs://" + bucket, ex);
        }
        return "https://storage.googleapis.com/" + bucket + "/" + object;
    }

    private String storeOnDisk(MultipartFile file, String filename) {
        try {
            Files.createDirectories(uploadDir);
            Path target = uploadDir.resolve(filename).toAbsolutePath().normalize();
            file.transferTo(target);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to store uploaded file", ex);
        }
        return publicBaseUrl + "/uploads/" + filename;
    }

    private String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        String ext = originalFilename.substring(dot);
        // Only keep simple, safe extensions (no path tricks).
        return ext.matches("\\.[A-Za-z0-9]{1,10}") ? ext.toLowerCase() : "";
    }

    private static String stripTrailingSlash(String url) {
        String trimmed = url == null ? "" : url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
