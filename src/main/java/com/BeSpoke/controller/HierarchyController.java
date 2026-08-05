package com.BeSpoke.controller;

import com.BeSpoke.dto.HierarchyDto;
import com.BeSpoke.service.CompanyService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** The whole platform org chart in one call — powers the admin dashboard. */
@RestController
public class HierarchyController {

    private final CompanyService companyService;

    public HierarchyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping("/api/hierarchy")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public HierarchyDto hierarchy() {
        return companyService.hierarchy();
    }
}
