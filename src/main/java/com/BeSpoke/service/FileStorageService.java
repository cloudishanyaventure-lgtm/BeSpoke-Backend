package com.BeSpoke.service;

import com.BeSpoke.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/** Stores uploaded images on local disk under ./uploads, served statically at /uploads/**. */
@Service
public class FileStorageService {

    static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5MB

    private final Path uploadDir = Paths.get("uploads");

    /** Validates and stores an image; returns its public URL path (e.g. "/uploads/<uuid>.png"). */
    public String storeImage(MultipartFile file) {
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
        try {
            Files.createDirectories(uploadDir);
            Path target = uploadDir.resolve(filename).toAbsolutePath().normalize();
            file.transferTo(target);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to store uploaded file", ex);
        }
        return "/uploads/" + filename;
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
}
