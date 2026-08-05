package com.BeSpoke.dto;

import java.time.Instant;

/** One row of the "Awaiting your approval" queue at the top of /studio/leads. */
public record PendingApprovalDto(
        Long drawingId,
        Long leadId,
        String contactName,
        String title,
        String floorLabel,
        String spaceLabel,
        String uploadedByName,
        Instant submittedAt
) {
}
