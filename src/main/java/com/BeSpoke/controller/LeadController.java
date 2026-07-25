package com.BeSpoke.controller;

import com.BeSpoke.dto.ActivityDto;
import com.BeSpoke.dto.AssignRequest;
import com.BeSpoke.dto.CreateActivityRequest;
import com.BeSpoke.dto.CreateLeadRequest;
import com.BeSpoke.dto.FollowUpRequest;
import com.BeSpoke.dto.LeadDetailDto;
import com.BeSpoke.dto.LeadSummaryDto;
import com.BeSpoke.dto.StageChangeRequest;
import com.BeSpoke.entity.User;
import com.BeSpoke.service.CurrentUserService;
import com.BeSpoke.service.LeadService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Staff lead funnel. Designers are scoped to their assigned leads in the service layer. */
@RestController
@RequestMapping("/api/leads")
public class LeadController {

    private final LeadService leadService;
    private final CurrentUserService currentUserService;

    public LeadController(LeadService leadService, CurrentUserService currentUserService) {
        this.leadService = leadService;
        this.currentUserService = currentUserService;
    }

    private User me(Authentication authentication) {
        return currentUserService.requireByEmail(authentication.getName());
    }

    @GetMapping
    public List<LeadSummaryDto> list(Authentication authentication,
                                     @RequestParam(required = false) String stage,
                                     @RequestParam(required = false) String q,
                                     @RequestParam(required = false) Long assigned) {
        return leadService.list(me(authentication), stage, q, assigned);
    }

    @GetMapping("/{id}")
    public LeadDetailDto detail(Authentication authentication, @PathVariable Long id) {
        return leadService.detail(me(authentication), id);
    }

    @PostMapping("/{id}/activities")
    public ResponseEntity<ActivityDto> addActivity(Authentication authentication,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody CreateActivityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(leadService.addActivity(me(authentication), id, request));
    }

    @PutMapping("/{id}/stage")
    public LeadSummaryDto changeStage(Authentication authentication,
                                      @PathVariable Long id,
                                      @Valid @RequestBody StageChangeRequest request) {
        return leadService.changeStage(me(authentication), id, request);
    }

    @PutMapping("/{id}/follow-up")
    public LeadSummaryDto setFollowUp(Authentication authentication,
                                      @PathVariable Long id,
                                      @RequestBody FollowUpRequest request) {
        return leadService.setFollowUp(me(authentication), id, request.at());
    }

    /** Admin: manual lead capture (walk-in / phone / referral). */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LeadSummaryDto> create(Authentication authentication,
                                                 @Valid @RequestBody CreateLeadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(leadService.createManual(me(authentication), request));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public LeadSummaryDto assign(Authentication authentication,
                                 @PathVariable Long id,
                                 @Valid @RequestBody AssignRequest request) {
        return leadService.assign(me(authentication), id, request.designerId());
    }
}
