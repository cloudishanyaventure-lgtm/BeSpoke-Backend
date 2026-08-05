package com.BeSpoke.controller;

import com.BeSpoke.dto.AuditEventDto;
import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.CompanyRepository;
import com.BeSpoke.service.AuditService;
import com.BeSpoke.service.CurrentUserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Audit feed: platform reads any/all companies; company staff are forced to their own. */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;
    private final CurrentUserService currentUserService;
    private final CompanyRepository companyRepository;

    public AuditController(AuditService auditService,
                           CurrentUserService currentUserService,
                           CompanyRepository companyRepository) {
        this.auditService = auditService;
        this.currentUserService = currentUserService;
        this.companyRepository = companyRepository;
    }

    @GetMapping
    public List<AuditEventDto> list(Authentication authentication,
                                    @RequestParam(required = false) Long companyId,
                                    @RequestParam(defaultValue = "50") int limit) {
        User current = currentUserService.requireByEmail(authentication.getName());
        Company company;
        if (current.getRole().isPlatform()) {
            company = companyId == null ? null
                    : companyRepository.findById(companyId)
                            .orElseThrow(() -> new NotFoundException("Company not found"));
        } else {
            company = current.getCompany();
            if (company == null) {
                throw new NotFoundException("You are not attached to a company");
            }
        }
        return auditService.list(company, limit);
    }
}
