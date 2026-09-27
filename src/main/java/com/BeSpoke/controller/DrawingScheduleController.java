package com.BeSpoke.controller;

import com.BeSpoke.dto.DrawingScheduleDto;
import com.BeSpoke.dto.DrawingScheduleRequest;
import com.BeSpoke.entity.User;
import com.BeSpoke.service.CurrentUserService;
import com.BeSpoke.service.DrawingScheduleService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The drawing timeline for a lead. Uploaders build/adjust/submit and track item progress;
 * approvers (design manager / director) approve and reopen. Lead scoping is in the service.
 */
@RestController
@RequestMapping("/api")
public class DrawingScheduleController {

    private static final String UPLOADERS =
            "hasAnyRole('SUPER_ADMIN','ADMIN','DIRECTOR','PRINCIPAL_ARCHITECT','DESIGN_MANAGER','DESIGNER','REMOTE_DESIGNER','PROJECT_MANAGER')";
    private static final String APPROVERS =
            "hasAnyRole('SUPER_ADMIN','ADMIN','DIRECTOR','PRINCIPAL_ARCHITECT','DESIGN_MANAGER')";

    private final DrawingScheduleService service;
    private final CurrentUserService currentUserService;

    public DrawingScheduleController(DrawingScheduleService service,
                                     CurrentUserService currentUserService) {
        this.service = service;
        this.currentUserService = currentUserService;
    }

    private User me(Authentication a) {
        return currentUserService.requireByEmail(a.getName());
    }

    @GetMapping("/leads/{id}/drawing-schedule")
    public DrawingScheduleDto get(Authentication a, @PathVariable Long id) {
        return service.get(me(a), id);
    }

    @PutMapping("/leads/{id}/drawing-schedule")
    @PreAuthorize(UPLOADERS)
    public DrawingScheduleDto save(Authentication a, @PathVariable Long id,
                                   @Valid @RequestBody DrawingScheduleRequest req) {
        return service.save(me(a), id, req);
    }

    @PostMapping("/leads/{id}/drawing-schedule/submit")
    @PreAuthorize(UPLOADERS)
    public DrawingScheduleDto submit(Authentication a, @PathVariable Long id) {
        return service.submit(me(a), id);
    }

    @PostMapping("/leads/{id}/drawing-schedule/approve")
    @PreAuthorize(APPROVERS)
    public DrawingScheduleDto approve(Authentication a, @PathVariable Long id) {
        return service.approve(me(a), id);
    }

    @PostMapping("/leads/{id}/drawing-schedule/reopen")
    @PreAuthorize(APPROVERS)
    public DrawingScheduleDto reopen(Authentication a, @PathVariable Long id) {
        return service.reopen(me(a), id);
    }

    @PostMapping("/leads/{id}/drawing-schedule/items/{itemId}/status")
    @PreAuthorize(UPLOADERS)
    public DrawingScheduleDto itemStatus(Authentication a, @PathVariable Long id,
                                         @PathVariable Long itemId, @RequestBody StatusRequest req) {
        return service.setItemStatus(me(a), id, itemId, req.status());
    }

    public record StatusRequest(String status) {
    }
}
