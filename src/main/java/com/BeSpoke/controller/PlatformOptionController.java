package com.BeSpoke.controller;

import com.BeSpoke.dto.PlatformOptionDto;
import com.BeSpoke.dto.PlatformOptionRequest;
import com.BeSpoke.service.CurrentUserService;
import com.BeSpoke.service.PlatformOptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Editable picklists: read by the whole site, written by platform admins. */
@RestController
public class PlatformOptionController {

    private final PlatformOptionService optionService;
    private final CurrentUserService currentUserService;

    public PlatformOptionController(PlatformOptionService optionService,
                                    CurrentUserService currentUserService) {
        this.optionService = optionService;
        this.currentUserService = currentUserService;
    }

    /** Every active list in one call — the site loads this once. */
    @GetMapping("/api/public/options")
    public Map<String, List<PlatformOptionDto>> publicOptions() {
        return optionService.publicLists();
    }

    @GetMapping("/api/options")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Map<String, List<PlatformOptionDto>> adminOptions() {
        return optionService.adminLists();
    }

    @PostMapping("/api/options")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<PlatformOptionDto> create(Authentication authentication,
                                                    @Valid @RequestBody PlatformOptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(optionService.create(
                currentUserService.requireByEmail(authentication.getName()), request));
    }

    @PutMapping("/api/options/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public PlatformOptionDto update(Authentication authentication, @PathVariable Long id,
                                    @Valid @RequestBody PlatformOptionRequest request) {
        return optionService.update(
                currentUserService.requireByEmail(authentication.getName()), id, request);
    }

    @DeleteMapping("/api/options/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
        optionService.delete(currentUserService.requireByEmail(authentication.getName()), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/options/{listKey}/order")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public List<PlatformOptionDto> reorder(Authentication authentication,
                                           @PathVariable String listKey,
                                           @RequestBody List<Long> orderedIds) {
        return optionService.reorder(
                currentUserService.requireByEmail(authentication.getName()), listKey, orderedIds);
    }
}
