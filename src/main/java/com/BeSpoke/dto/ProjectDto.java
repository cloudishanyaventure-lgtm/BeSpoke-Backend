package com.BeSpoke.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Flat project summary matching the frontend ProjectSummary contract.
 * budget is admin-only (null otherwise); milestones is populated only
 * where the endpoint contract says so (both omitted from JSON when null).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProjectDto(
        Long id,
        Long leadId,
        String name,
        String stage,
        String health,
        String clientName,
        String designerName,
        BigDecimal budget,
        LocalDate startDate,
        LocalDate targetDate,
        int completionPct,
        List<MilestoneDto> milestones
) {
}
