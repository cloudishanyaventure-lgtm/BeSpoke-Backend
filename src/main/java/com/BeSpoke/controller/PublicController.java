package com.BeSpoke.controller;

import com.BeSpoke.dto.EnquiryRequest;
import com.BeSpoke.dto.PublicDesignerDto;
import com.BeSpoke.dto.PublicStudioDto;
import com.BeSpoke.entity.CompanyType;
import com.BeSpoke.service.LeadService;
import com.BeSpoke.service.PublicProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Public marketing-site endpoints - no authentication. */
@RestController
public class PublicController {

    private final LeadService leadService;
    private final PublicProfileService publicProfileService;

    public PublicController(LeadService leadService, PublicProfileService publicProfileService) {
        this.leadService = leadService;
        this.publicProfileService = publicProfileService;
    }

    @GetMapping("/api/public/designers")
    public List<PublicDesignerDto> designers() {
        return publicProfileService.designers();
    }

    @GetMapping("/api/public/designers/{id}")
    public PublicDesignerDto designer(@PathVariable Long id) {
        return publicProfileService.designer(id);
    }

    /** Profile-complete studios for the public directory and the signup/enquiry picker. */
    @GetMapping("/api/public/studios")
    public List<PublicStudioDto> studios() {
        return publicProfileService.studios();
    }

    @GetMapping("/api/public/studios/{slug}")
    public PublicStudioDto studio(@PathVariable String slug) {
        return publicProfileService.profile(slug, CompanyType.DESIGN);
    }

    @GetMapping("/api/public/vendors")
    public List<PublicStudioDto> vendors() {
        return publicProfileService.vendors();
    }

    @GetMapping("/api/public/vendors/{slug}")
    public PublicStudioDto vendor(@PathVariable String slug) {
        return publicProfileService.profile(slug, CompanyType.VENDOR);
    }

    @PostMapping("/api/enquiries")
    public ResponseEntity<Map<String, Object>> createEnquiry(@Valid @RequestBody EnquiryRequest request) {
        Long leadId = leadService.createEnquiry(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "leadId", leadId,
                "message", "Thanks for reaching out — the BeSpoke team will get back to you shortly."));
    }
}
