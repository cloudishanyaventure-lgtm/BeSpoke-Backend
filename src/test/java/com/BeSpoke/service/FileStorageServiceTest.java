package com.BeSpoke.service;

import com.BeSpoke.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Uploads are a trust boundary — anything authenticated can post bytes here — and the URL
 * this returns is written to the database and read back by a different origin. Both are
 * worth pinning down.
 */
class FileStorageServiceTest {

    /** No bucket configured, so this exercises the local-disk path. */
    private final FileStorageService service =
            new FileStorageService("", "https://crm.bespokedesign.in/");

    private static MockMultipartFile image(String name, String type, int bytes) {
        return new MockMultipartFile("file", name, type, new byte[bytes]);
    }

    /**
     * The bug this replaced: a relative "/uploads/x.png" resolves against whatever site
     * rendered the page, so every CRM-uploaded image 404'd on the public website.
     */
    @Test
    void aStoredImageComesBackAsAnAbsoluteUrl() throws Exception {
        String url = service.storeImage(image("logo.PNG", "image/png", 16));
        try {
            assertTrue(url.startsWith("https://crm.bespokedesign.in/uploads/"),
                    "expected an absolute URL, got " + url);
            // The configured base had a trailing slash; the URL must not double it up.
            assertEquals(-1, url.indexOf("//uploads"));
            // Extension is kept but lower-cased, and the name is a UUID, never the caller's.
            assertTrue(url.endsWith(".png"), url);
            assertEquals(-1, url.indexOf("logo"));
        } finally {
            Path written = Paths.get("uploads").resolve(url.substring(url.lastIndexOf('/') + 1));
            Files.deleteIfExists(written);
        }
    }

    @Test
    void anythingThatIsNotAnImageIsRefused() {
        assertThrows(BadRequestException.class,
                () -> service.storeImage(image("payload.svg", "text/html", 16)));
        assertThrows(BadRequestException.class,
                () -> service.storeImage(image("notes.pdf", "application/pdf", 16)));
    }

    @Test
    void anEmptyOrOversizeFileIsRefused() {
        assertThrows(BadRequestException.class,
                () -> service.storeImage(image("empty.png", "image/png", 0)));
        assertThrows(BadRequestException.class, () -> service.storeImage(
                image("huge.png", "image/png", (int) FileStorageService.MAX_FILE_SIZE_BYTES + 1)));
    }

    byte[] model(String uri) {
        String json = "{\"asset\":{\"version\":\"2.0\"},\"meshes\":[{\"primitives\":[]}],\"buffers\":[{\"uri\":\"" + uri + "\"}]}";
        while (json.length() % 4 != 0) json += " ";
        byte[] chunk = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return java.nio.ByteBuffer.allocate(20 + chunk.length).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                .putInt(0x46546c67).putInt(2).putInt(20 + chunk.length).putInt(chunk.length).putInt(0x4e4f534a).put(chunk).array();
    }
    @Test void modelsRequireGlbHeaderAndEmbeddedResources() throws Exception {
        assertThrows(BadRequestException.class, () -> service.storeModel(image("fake.glb", "model/gltf-binary", 100)));
        assertThrows(BadRequestException.class, () -> service.storeModel(new MockMultipartFile("file", "remote.glb", "model/gltf-binary", model("https://example.com/private.bin"))));
        String url = service.storeModel(new MockMultipartFile("file", "table.glb", "application/octet-stream", model("data:application/octet-stream;base64,AAAA")));
        try { assertTrue(url.endsWith(".glb")); }
        finally { Files.deleteIfExists(Paths.get("uploads").resolve(url.substring(url.lastIndexOf('/') + 1))); }
    }
}
