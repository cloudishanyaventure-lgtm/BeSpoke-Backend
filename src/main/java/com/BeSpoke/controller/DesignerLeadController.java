package com.BeSpoke.controller;

import com.BeSpoke.dto.DesignerStatsDto;
import com.BeSpoke.dto.LeadDto;
import com.BeSpoke.dto.UpdateLeadStatusRequest;
import com.BeSpoke.entity.User;
import com.BeSpoke.service.CurrentUserService;
import com.BeSpoke.service.LeadService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/designer")
public class DesignerLeadController {

    private final LeadService leadService;
    private final CurrentUserService currentUserService;

    public DesignerLeadController(LeadService leadService, CurrentUserService currentUserService) {
        this.leadService = leadService;
        this.currentUserService = currentUserService;
    }

    /** Leads assigned to me, pending my approval. */
    @GetMapping("/leads")
    public List<LeadDto> myLeads(Authentication authentication) {
        User designer = currentUserService.requireByEmail(authentication.getName());
        return leadService.designerPendingLeads(designer);
    }

    @PostMapping("/leads/{id}/approve")
    public LeadDto approve(Authentication authentication, @PathVariable Long id) {
        User designer = currentUserService.requireByEmail(authentication.getName());
        return leadService.approveLead(designer, id);
    }

    @PostMapping("/leads/{id}/reject")
    public LeadDto reject(Authentication authentication, @PathVariable Long id) {
        User designer = currentUserService.requireByEmail(authentication.getName());
        return leadService.rejectLead(designer, id);
    }

    /** Approved / in-progress / completed projects. */
    @GetMapping("/projects")
    public List<LeadDto> myProjects(Authentication authentication) {
        User designer = currentUserService.requireByEmail(authentication.getName());
        return leadService.designerProjects(designer);
    }

    /** Move my project forward: APPROVED -> IN_PROGRESS -> COMPLETED. */
    @PostMapping("/leads/{id}/status")
    public LeadDto updateStatus(Authentication authentication,
                                @PathVariable Long id,
                                @Valid @RequestBody UpdateLeadStatusRequest request) {
        User designer = currentUserService.requireByEmail(authentication.getName());
        return leadService.updateDesignerLeadStatus(designer, id, request.status());
    }

    /** Dashboard stats for the authenticated designer. */
    @GetMapping("/stats")
    public DesignerStatsDto stats(Authentication authentication) {
        User designer = currentUserService.requireByEmail(authentication.getName());
        return leadService.designerStats(designer);
    }
}
