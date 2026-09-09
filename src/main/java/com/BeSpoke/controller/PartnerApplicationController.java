package com.BeSpoke.controller;

import com.BeSpoke.dto.ApprovePartnerRequest;
import com.BeSpoke.dto.PartnerApplicationDto;
import com.BeSpoke.dto.PartnerApplicationRequest;
import com.BeSpoke.service.CompanyService;
import com.BeSpoke.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Partner sign-up: a public form in, a platform-admin decision out. */
@RestController
@RequestMapping("/api/partner-applications")
public class PartnerApplicationController {

    private final CompanyService companyService;
    private final CurrentUserService currentUserService;

    public PartnerApplicationController(CompanyService companyService,
                                        CurrentUserService currentUserService) {
        this.companyService = companyService;
        this.currentUserService = currentUserService;
    }

    /** Public — no account exists yet, and none is created until approval. */
    @PostMapping
    public ResponseEntity<Map<String, Object>> apply(
            @Valid @RequestBody PartnerApplicationRequest request) {
        Long id = companyService.applyAsPartner(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "applicationId", id,
                "message", "Thanks — your application is with the BeSpoke team."
                        + " We'll email you the moment it's approved."));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public List<PartnerApplicationDto> list() {
        return companyService.applications();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public PartnerApplicationDto approve(Authentication authentication,
                                         @PathVariable Long id,
                                         @Valid @RequestBody ApprovePartnerRequest request) {
        return companyService.approveApplication(
                currentUserService.requireByEmail(authentication.getName()), id, request);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public PartnerApplicationDto reject(Authentication authentication,
                                        @PathVariable Long id,
                                        @RequestParam(required = false) String note) {
        return companyService.rejectApplication(
                currentUserService.requireByEmail(authentication.getName()), id, note);
    }
}
