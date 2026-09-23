package com.BeSpoke.controller;

import com.BeSpoke.dto.ActivityDto;
import com.BeSpoke.dto.AssignRequest;
import com.BeSpoke.dto.AssignSalesRequest;
import com.BeSpoke.dto.CreateActivityRequest;
import com.BeSpoke.dto.CreateLeadRequest;
import com.BeSpoke.dto.FollowUpRequest;
import com.BeSpoke.dto.LeadDetailDto;
import com.BeSpoke.dto.LeadSummaryDto;
import com.BeSpoke.dto.RequirementFormDto;
import com.BeSpoke.dto.RequirementFormRequest;
import com.BeSpoke.dto.RoomRequest;
import com.BeSpoke.dto.RouteLeadRequest;
import com.BeSpoke.dto.StageChangeRequest;
import com.BeSpoke.dto.UpdateLeadContactRequest;
import com.BeSpoke.entity.User;
import com.BeSpoke.service.CurrentUserService;
import com.BeSpoke.service.LeadService;
import com.BeSpoke.service.RequirementService;
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

    /**
     * Everyone who actually works a lead — including the consultant or sales exec who makes
     * the first call and takes the brief down over the phone. Visibility is still the
     * service layer's job; this only keeps customers and vendors out.
     */
    private static final String WORKS_THE_LEAD = "hasAnyRole('SUPER_ADMIN','ADMIN','DIRECTOR',"
            + "'PRINCIPAL_ARCHITECT','DESIGN_MANAGER','DESIGNER','REMOTE_DESIGNER','PROJECT_MANAGER',"
            + "'SALES_MANAGER','CUSTOMER_CONSULTANT','SALES_EXECUTIVE')";

    private final LeadService leadService;
    private final RequirementService requirementService;
    private final CurrentUserService currentUserService;

    public LeadController(LeadService leadService,
                          RequirementService requirementService,
                          CurrentUserService currentUserService) {
        this.leadService = leadService;
        this.requirementService = requirementService;
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

    /** Platform dashboard counters — spares the admin page loading every lead. */
    @GetMapping("/stats")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public java.util.Map<String, Long> stats() {
        return leadService.platformStats();
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

    /** Staff filling in contact details the customer never completed; mirrors onto their account. */
    @PutMapping("/{id}/contact")
    @PreAuthorize(WORKS_THE_LEAD)
    public LeadSummaryDto updateContact(Authentication authentication,
                                        @PathVariable Long id,
                                        @Valid @RequestBody UpdateLeadContactRequest request) {
        return leadService.updateContact(me(authentication), id, request);
    }

    @PutMapping("/{id}/follow-up")
    public LeadSummaryDto setFollowUp(Authentication authentication,
                                      @PathVariable Long id,
                                      @RequestBody FollowUpRequest request) {
        return leadService.setFollowUp(me(authentication), id, request.at());
    }

    /** Manual lead capture (walk-in / phone / referral) - platform admin or studio funnel roles. */
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DIRECTOR','DESIGN_MANAGER','SALES_MANAGER','DESIGNER','REMOTE_DESIGNER')")
    public ResponseEntity<LeadSummaryDto> create(Authentication authentication,
                                                 @Valid @RequestBody CreateLeadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(leadService.createManual(me(authentication), request));
    }

    /** Senior sign-off on a designer/design-manager-captured lead (must outrank the creator). */
    @PutMapping("/{id}/approve-creation")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DIRECTOR','PRINCIPAL_ARCHITECT','DESIGN_MANAGER')")
    public LeadSummaryDto approveCreation(Authentication authentication, @PathVariable Long id) {
        return leadService.approveCreation(me(authentication), id);
    }

    /** The studio staffs its own lead — BeSpoke only routes it there (see /route). */
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('DIRECTOR','DESIGN_MANAGER','SALES_MANAGER')")
    public LeadSummaryDto assign(Authentication authentication,
                                 @PathVariable Long id,
                                 @Valid @RequestBody AssignRequest request) {
        return leadService.assign(me(authentication), id, request.designerId());
    }

    /** Director/sales manager gives the customer relationship to a consultant / sales exec. */
    @PutMapping("/{id}/assign-sales")
    @PreAuthorize("hasAnyRole('DIRECTOR','SALES_MANAGER')")
    public LeadSummaryDto assignSales(Authentication authentication,
                                      @PathVariable Long id,
                                      @Valid @RequestBody AssignSalesRequest request) {
        return leadService.assignSales(me(authentication), id, request.userId());
    }

    /** Platform transfers a lead from the pool to a design studio. */
    @PutMapping("/{id}/route")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public LeadSummaryDto route(Authentication authentication,
                                @PathVariable Long id,
                                @Valid @RequestBody RouteLeadRequest request) {
        return leadService.route(me(authentication), id, request.companyId());
    }

    /** Receiving studio accepts a transferred lead. */
    @PutMapping("/{id}/accept")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DIRECTOR','SALES_MANAGER')")
    public LeadSummaryDto accept(Authentication authentication, @PathVariable Long id) {
        return leadService.accept(me(authentication), id);
    }

    /** Staff PRD editor — replaces the brief's rooms, skipping the customer quote lock. */
    @PutMapping("/{id}/prd/rooms")
    @PreAuthorize(WORKS_THE_LEAD)
    public RequirementFormDto replacePrdRooms(Authentication authentication,
                                              @PathVariable Long id,
                                              @Valid @RequestBody List<RoomRequest> rooms) {
        User staff = me(authentication);
        return requirementService.staffReplaceRooms(leadService.scopedLead(staff, id), rooms);
    }

    /** Staff PRD editor — the brief's scalar sections, same fields as the customer wizard. */
    @PutMapping("/{id}/prd/form")
    @PreAuthorize(WORKS_THE_LEAD)
    public RequirementFormDto upsertPrdForm(Authentication authentication,
                                            @PathVariable Long id,
                                            @Valid @RequestBody RequirementFormRequest request) {
        User staff = me(authentication);
        return requirementService.staffUpsertForm(leadService.scopedLead(staff, id), request);
    }

    /** Marks the brief complete when the studio captured it — stops the customer being asked again. */
    @PostMapping("/{id}/prd/submit")
    @PreAuthorize(WORKS_THE_LEAD)
    public RequirementFormDto submitPrd(Authentication authentication, @PathVariable Long id) {
        User staff = me(authentication);
        return requirementService.staffSubmit(leadService.scopedLead(staff, id), staff);
    }

    /** Studio's final sign-off on the brief — locks it for everyone. */
    @PostMapping("/{id}/prd/approve")
    @PreAuthorize(WORKS_THE_LEAD)
    public RequirementFormDto approvePrd(Authentication authentication, @PathVariable Long id) {
        User staff = me(authentication);
        return requirementService.studioApprove(leadService.scopedLead(staff, id), staff);
    }

    /** Sends phase two — the PRD — to the customer for sign-off. */
    @PostMapping("/{id}/prd/send-review")
    @PreAuthorize(WORKS_THE_LEAD)
    public RequirementFormDto sendPrdForReview(Authentication authentication, @PathVariable Long id) {
        User staff = me(authentication);
        return requirementService.sendPrdForReview(leadService.scopedLead(staff, id), staff);
    }

    /** Takes a locked brief back off the shelf so the PRD can be worked again. */
    @PostMapping("/{id}/prd/reopen")
    @PreAuthorize(WORKS_THE_LEAD)
    public RequirementFormDto reopenPrd(Authentication authentication, @PathVariable Long id) {
        User staff = me(authentication);
        return requirementService.studioReopen(leadService.scopedLead(staff, id), staff);
    }
}
