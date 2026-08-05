package com.BeSpoke.controller;

import com.BeSpoke.dto.CreateDrawingRequest;
import com.BeSpoke.dto.DrawingDto;
import com.BeSpoke.dto.PendingApprovalDto;
import com.BeSpoke.dto.PrdSpaceDto;
import com.BeSpoke.dto.RejectDrawingRequest;
import com.BeSpoke.entity.User;
import com.BeSpoke.service.CurrentUserService;
import com.BeSpoke.service.DrawingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Drawing WIP → approval pipeline. Uploader roles create/submit, approver
 * roles (canApproveDrawings) approve/reject/finalize, the customer watches
 * the whole pipeline at /api/my/drawings.
 */
@RestController
@RequestMapping("/api")
public class DrawingController {

    private static final String UPLOADERS =
            "hasAnyRole('SUPER_ADMIN','ADMIN','DIRECTOR','PRINCIPAL_ARCHITECT','DESIGN_MANAGER','DESIGNER','PROJECT_MANAGER')";
    private static final String APPROVERS =
            "hasAnyRole('SUPER_ADMIN','ADMIN','DIRECTOR','PRINCIPAL_ARCHITECT','DESIGN_MANAGER')";

    private final DrawingService drawingService;
    private final CurrentUserService currentUserService;

    public DrawingController(DrawingService drawingService, CurrentUserService currentUserService) {
        this.drawingService = drawingService;
        this.currentUserService = currentUserService;
    }

    private User me(Authentication authentication) {
        return currentUserService.requireByEmail(authentication.getName());
    }

    @GetMapping("/leads/{id}/drawings")
    public List<DrawingDto> list(Authentication authentication, @PathVariable Long id) {
        return drawingService.list(me(authentication), id);
    }

    /** The PRD space picker for the upload form. Lives here to sit next to the upload. */
    @GetMapping("/leads/{id}/prd-spaces")
    public List<PrdSpaceDto> prdSpaces(Authentication authentication, @PathVariable Long id) {
        return drawingService.prdSpaces(me(authentication), id);
    }

    /**
     * "Awaiting your approval". Deliberately unannotated: the service returns an empty
     * list for roles that cannot approve, so the studio page can always ask.
     */
    @GetMapping("/drawings/pending")
    public List<PendingApprovalDto> pending(Authentication authentication) {
        return drawingService.pending(me(authentication));
    }

    @PostMapping("/leads/{id}/drawings")
    @PreAuthorize(UPLOADERS)
    public ResponseEntity<DrawingDto> create(Authentication authentication,
                                             @PathVariable Long id,
                                             @Valid @RequestBody CreateDrawingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(drawingService.create(me(authentication), id, request));
    }

    @PostMapping("/drawings/{id}/submit")
    @PreAuthorize(UPLOADERS)
    public DrawingDto submit(Authentication authentication, @PathVariable Long id) {
        return drawingService.submit(me(authentication), id);
    }

    @PostMapping("/drawings/{id}/approve")
    @PreAuthorize(APPROVERS)
    public DrawingDto approve(Authentication authentication, @PathVariable Long id) {
        return drawingService.approve(me(authentication), id);
    }

    @PostMapping("/drawings/{id}/reject")
    @PreAuthorize(APPROVERS)
    public DrawingDto reject(Authentication authentication,
                             @PathVariable Long id,
                             @Valid @RequestBody RejectDrawingRequest request) {
        return drawingService.reject(me(authentication), id, request.reason());
    }

    @PostMapping("/drawings/{id}/finalize")
    @PreAuthorize(APPROVERS)
    public DrawingDto finalize(Authentication authentication, @PathVariable Long id) {
        return drawingService.finalize(me(authentication), id);
    }

    /** Customer: all drawings of my lead — URL security scopes /api/my/** to CUSTOMER. */
    @GetMapping("/my/drawings")
    public List<DrawingDto> myDrawings(Authentication authentication) {
        return drawingService.myDrawings(me(authentication));
    }

    /** Customer sign-off: APPROVED → FINAL. */
    @PostMapping("/my/drawings/{id}/approve")
    public DrawingDto customerApprove(Authentication authentication, @PathVariable Long id) {
        return drawingService.customerApprove(me(authentication), id);
    }

    /** Customer sends an APPROVED drawing back to WIP with a reason. */
    @PostMapping("/my/drawings/{id}/request-changes")
    public DrawingDto customerRequestChanges(Authentication authentication,
                                             @PathVariable Long id,
                                             @Valid @RequestBody RejectDrawingRequest request) {
        return drawingService.customerRequestChanges(me(authentication), id, request.reason());
    }
}
