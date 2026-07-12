package com.BeSpoke.controller;

import com.BeSpoke.dto.AdminOverviewDto;
import com.BeSpoke.dto.LeadDto;
import com.BeSpoke.service.LeadService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final LeadService leadService;

    public AdminController(LeadService leadService) {
        this.leadService = leadService;
    }

    @GetMapping("/leads")
    public List<LeadDto> leads(@RequestParam(required = false) String status) {
        return leadService.adminLeads(status);
    }

    @PostMapping("/leads/{id}/assign/{designerId}")
    public LeadDto assign(@PathVariable Long id, @PathVariable Long designerId) {
        return leadService.assignDesigner(id, designerId);
    }

    @GetMapping("/overview")
    public AdminOverviewDto overview() {
        return leadService.overview();
    }
}
