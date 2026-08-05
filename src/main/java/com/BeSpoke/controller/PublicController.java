package com.BeSpoke.controller;

import com.BeSpoke.dto.EnquiryRequest;
import com.BeSpoke.dto.PublicDesignerDto;
import com.BeSpoke.dto.PublicStudioDto;
import com.BeSpoke.service.CompanyService;
import com.BeSpoke.service.LeadService;
import com.BeSpoke.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Public marketing-site endpoints - no authentication. */
@RestController
public class PublicController {

    private final TeamService teamService;
    private final LeadService leadService;
    private final CompanyService companyService;

    public PublicController(TeamService teamService, LeadService leadService,
                            CompanyService companyService) {
        this.teamService = teamService;
        this.leadService = leadService;
        this.companyService = companyService;
    }

    @GetMapping("/api/public/designers")
    public List<PublicDesignerDto> designers() {
        return teamService.publicDesigners();
    }

    /** Active studios for the public directory and the signup/enquiry studio picker. */
    @GetMapping("/api/public/studios")
    public List<PublicStudioDto> studios() {
        return companyService.publicStudios();
    }

    @PostMapping("/api/enquiries")
    public ResponseEntity<Map<String, Object>> createEnquiry(@Valid @RequestBody EnquiryRequest request) {
        Long leadId = leadService.createEnquiry(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "leadId", leadId,
                "message", "Thanks for reaching out — the BeSpoke team will get back to you shortly."));
    }
}
