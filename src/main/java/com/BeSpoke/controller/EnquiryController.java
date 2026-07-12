package com.BeSpoke.controller;

import com.BeSpoke.dto.EnquiryRequest;
import com.BeSpoke.dto.LeadDto;
import com.BeSpoke.entity.User;
import com.BeSpoke.repository.UserRepository;
import com.BeSpoke.service.LeadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EnquiryController {

    private final LeadService leadService;
    private final UserRepository userRepository;

    public EnquiryController(LeadService leadService, UserRepository userRepository) {
        this.leadService = leadService;
        this.userRepository = userRepository;
    }

    /** Free enquiry - anonymous allowed. If a logged-in user submits, the lead is linked to them. */
    @PostMapping("/api/enquiries")
    public ResponseEntity<LeadDto> createEnquiry(@Valid @RequestBody EnquiryRequest request,
                                                 Authentication authentication) {
        User customer = null;
        if (authentication != null && authentication.isAuthenticated()) {
            customer = userRepository.findByEmail(authentication.getName()).orElse(null);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(leadService.createEnquiry(request, customer));
    }
}
