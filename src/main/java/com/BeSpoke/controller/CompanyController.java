package com.BeSpoke.controller;

import com.BeSpoke.dto.CompanyDto;
import com.BeSpoke.dto.CompanyRolesRequest;
import com.BeSpoke.dto.CreateCompanyRequest;
import com.BeSpoke.dto.KycStatusRequest;
import com.BeSpoke.dto.OrgMemberDto;
import com.BeSpoke.dto.UpdateCompanyRequest;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Company onboarding + KYC (platform) and company self-management (director). */
@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;
    private final CurrentUserService currentUserService;

    public CompanyController(CompanyService companyService, CurrentUserService currentUserService) {
        this.companyService = companyService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<CompanyDto> create(Authentication authentication,
                                             @Valid @RequestBody CreateCompanyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.create(
                currentUserService.requireByEmail(authentication.getName()), request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public List<CompanyDto> list() {
        return companyService.list();
    }

    @GetMapping("/mine")
    public CompanyDto mine(Authentication authentication) {
        return companyService.mine(currentUserService.requireByEmail(authentication.getName()));
    }

    @GetMapping("/{id}/org")
    public List<OrgMemberDto> org(Authentication authentication, @PathVariable Long id) {
        return companyService.org(currentUserService.requireByEmail(authentication.getName()), id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DIRECTOR')")
    public CompanyDto update(Authentication authentication,
                             @PathVariable Long id,
                             @Valid @RequestBody UpdateCompanyRequest request) {
        return companyService.update(
                currentUserService.requireByEmail(authentication.getName()), id, request);
    }

    @PutMapping("/{id}/kyc-status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public CompanyDto updateKycStatus(Authentication authentication,
                                      @PathVariable Long id,
                                      @Valid @RequestBody KycStatusRequest request) {
        return companyService.updateKycStatus(
                currentUserService.requireByEmail(authentication.getName()), id, request.status());
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DIRECTOR')")
    public CompanyDto configureRoles(Authentication authentication,
                                     @PathVariable Long id,
                                     @Valid @RequestBody CompanyRolesRequest request) {
        return companyService.configureRoles(
                currentUserService.requireByEmail(authentication.getName()), id, request);
    }
}
