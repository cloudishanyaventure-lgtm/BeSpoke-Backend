package com.BeSpoke.controller;

import com.BeSpoke.service.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final FileStorageService fileStorageService;

    public UploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /** Any authenticated user may upload an image (max 5MB); returns {"url": "/uploads/<file>"}. */
    @PostMapping
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.storeImage(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("url", url));
    }

    @PostMapping("/models")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DIRECTOR','PRODUCT_MANAGER','PRODUCT_SME')")
    public ResponseEntity<Map<String, String>> uploadModel(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("url", fileStorageService.storeModel(file)));
    }
}
