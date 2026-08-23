package com.BeSpoke.controller;

import com.BeSpoke.dto.PolicyDto;
import com.BeSpoke.dto.PolicyRequest;
import com.BeSpoke.service.CurrentUserService;
import com.BeSpoke.service.PolicyService;
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

/** Legal documents: read by anyone, written by platform admins. */
@RestController
public class PolicyController {

    private final PolicyService policyService;
    private final CurrentUserService currentUserService;

    public PolicyController(PolicyService policyService, CurrentUserService currentUserService) {
        this.policyService = policyService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/api/public/policies")
    public List<PolicyDto> published() {
        return policyService.published();
    }

    @GetMapping("/api/public/policies/{slug}")
    public PolicyDto policy(@PathVariable String slug) {
        return policyService.publicBySlug(slug);
    }

    @GetMapping("/api/policies")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public List<PolicyDto> all() {
        return policyService.all();
    }

    @PostMapping("/api/policies")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<PolicyDto> create(Authentication authentication,
                                            @Valid @RequestBody PolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.create(
                currentUserService.requireByEmail(authentication.getName()), request));
    }

    @PutMapping("/api/policies/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public PolicyDto update(Authentication authentication, @PathVariable Long id,
                            @Valid @RequestBody PolicyRequest request) {
        return policyService.update(
                currentUserService.requireByEmail(authentication.getName()), id, request);
    }

    @DeleteMapping("/api/policies/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
        policyService.delete(currentUserService.requireByEmail(authentication.getName()), id);
        return ResponseEntity.noContent().build();
    }
}
