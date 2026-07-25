package com.BeSpoke.controller;

import com.BeSpoke.dto.EnquiryRequest;
import com.BeSpoke.dto.PublicDesignerDto;
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

    public PublicController(TeamService teamService, LeadService leadService) {
        this.teamService = teamService;
        this.leadService = leadService;
    }

    @GetMapping("/api/public/designers")
    public List<PublicDesignerDto> designers() {
        return teamService.publicDesigners();
    }

    @PostMapping("/api/enquiries")
    public ResponseEntity<Map<String, Object>> createEnquiry(@Valid @RequestBody EnquiryRequest request) {
        Long leadId = leadService.createEnquiry(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "leadId", leadId,
                "message", "Thanks for reaching out — the DesignConnect team will get back to you shortly."));
    }
}
