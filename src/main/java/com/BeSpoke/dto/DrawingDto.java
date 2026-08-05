package com.BeSpoke.dto;

import com.BeSpoke.entity.Drawing;
import com.BeSpoke.entity.DrawingStatus;
import com.BeSpoke.entity.Role;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/** pendingWith = role the drawing awaits when PENDING_APPROVAL, e.g. "DESIGN_MANAGER". */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DrawingDto(
        Long id,
        Long leadId,
        String title,
        String floorLabel,
        String spaceLabel,
        String fileUrl,
        String notes,
        String status,
        String uploadedByName,
        Instant submittedAt,
        String approvedByName,
        Instant approvedAt,
        Instant customerApprovedAt,
        String rejectionReason,
        String pendingWith,
        Long requirementRoomId,
        Instant createdAt
) {

    /** Approval chain, most junior approver first. */
    private static final List<Role> APPROVAL_CHAIN =
            List.of(Role.DESIGN_MANAGER, Role.PRINCIPAL_ARCHITECT, Role.DIRECTOR);

    public static DrawingDto from(Drawing drawing) {
        return new DrawingDto(
                drawing.getId(),
                drawing.getLead().getId(),
                drawing.getTitle(),
                drawing.getFloorLabel(),
                drawing.getSpaceLabel(),
                drawing.getFileUrl(),
                drawing.getNotes(),
                drawing.getStatus().name(),
                drawing.getUploadedByName(),
                drawing.getSubmittedAt(),
                drawing.getApprovedByName(),
                drawing.getApprovedAt(),
                drawing.getCustomerApprovedAt(),
                drawing.getRejectionReason(),
                pendingWith(drawing),
                drawing.getRequirementRoomId(),
                drawing.getCreatedAt()
        );
    }

    /**
     * First enabled role in the approval chain; DIRECTOR can never be disabled.
     * Null unless the drawing is actually pending. Public so the approvals queue
     * (DrawingService.pending) routes by exactly the same rule the UI displays.
     */
    public static String pendingWith(Drawing drawing) {
        if (drawing.getStatus() != DrawingStatus.PENDING_APPROVAL || drawing.getLead().getCompany() == null) {
            return null;
        }
        Set<Role> enabled = drawing.getLead().getCompany().effectiveEnabledRoles();
        for (Role role : APPROVAL_CHAIN) {
            if (enabled.contains(role)) {
                return role.name();
            }
        }
        return Role.DIRECTOR.name();
    }
}
