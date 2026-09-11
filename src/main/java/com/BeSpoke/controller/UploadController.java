package com.BeSpoke.controller;

import com.BeSpoke.service.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    /** Abuse brake, not accounting: nobody curates 200 images a day by hand. */
    private static final int DAILY_LIMIT = 200;

    // ponytail: in-memory per-node counter, resets on restart; move to Redis/DB if multi-node.
    private final ConcurrentHashMap<String, AtomicInteger> uploadsToday = new ConcurrentHashMap<>();

    private final FileStorageService fileStorageService;

    public UploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /** Staff-only (SecurityConfig); image max 5MB; returns {"url": "/uploads/<file>"}. */
    @PostMapping
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                                      java.security.Principal principal) {
        String key = principal.getName() + ":" + LocalDate.now();
        if (uploadsToday.computeIfAbsent(key, k -> {
            uploadsToday.keySet().removeIf(old -> !old.endsWith(LocalDate.now().toString()));
            return new AtomicInteger();
        }).incrementAndGet() > DAILY_LIMIT) {
            throw new com.BeSpoke.exception.BadRequestException(
                    "Daily upload limit reached. Try again tomorrow.");
        }
        String url = fileStorageService.storeImage(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("url", url));
    }

    @PostMapping("/models")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DIRECTOR','PRODUCT_MANAGER','PRODUCT_SME')")
    public ResponseEntity<Map<String, String>> uploadModel(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("url", fileStorageService.storeModel(file)));
    }
}
