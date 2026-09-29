package com.BeSpoke.controller;

import com.BeSpoke.dto.DrawingScheduleDto;
import com.BeSpoke.dto.DrawingScheduleRequest;
import com.BeSpoke.entity.DrawingSchedule;
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
 * The project timelines for a lead. Uploaders build/adjust/submit and track item progress;
 * approvers (design manager / director) approve and reopen. Lead scoping is in the service.
 *
 * <p>One set of endpoints serves both timelines: the path segment is the kind, so
 * /drawing-schedule and /execution-schedule are the same code on different rows.
 */
@RestController
@RequestMapping("/api/leads/{id}/{kind:drawing-schedule|execution-schedule}")
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

    private static DrawingSchedule.Kind kindOf(String path) {
        return path.startsWith("execution")
                ? DrawingSchedule.Kind.EXECUTION
                : DrawingSchedule.Kind.DRAWING;
    }

    @GetMapping
    public DrawingScheduleDto get(Authentication a, @PathVariable Long id,
                                  @PathVariable String kind) {
        return service.get(me(a), id, kindOf(kind));
    }

    @PutMapping
    @PreAuthorize(UPLOADERS)
    public DrawingScheduleDto save(Authentication a, @PathVariable Long id,
                                   @PathVariable String kind,
                                   @Valid @RequestBody DrawingScheduleRequest req) {
        return service.save(me(a), id, kindOf(kind), req);
    }

    @PostMapping("/submit")
    @PreAuthorize(UPLOADERS)
    public DrawingScheduleDto submit(Authentication a, @PathVariable Long id,
                                     @PathVariable String kind) {
        return service.submit(me(a), id, kindOf(kind));
    }

    @PostMapping("/approve")
    @PreAuthorize(APPROVERS)
    public DrawingScheduleDto approve(Authentication a, @PathVariable Long id,
                                      @PathVariable String kind) {
        return service.approve(me(a), id, kindOf(kind));
    }

    @PostMapping("/reopen")
    @PreAuthorize(APPROVERS)
    public DrawingScheduleDto reopen(Authentication a, @PathVariable Long id,
                                     @PathVariable String kind) {
        return service.reopen(me(a), id, kindOf(kind));
    }

    @PostMapping("/items/{itemId}/status")
    @PreAuthorize(UPLOADERS)
    public DrawingScheduleDto itemStatus(Authentication a, @PathVariable Long id,
                                         @PathVariable String kind, @PathVariable Long itemId,
                                         @RequestBody StatusRequest req) {
        return service.setItemStatus(me(a), id, kindOf(kind), itemId, req.status());
    }

    public record StatusRequest(String status) {
    }
}
