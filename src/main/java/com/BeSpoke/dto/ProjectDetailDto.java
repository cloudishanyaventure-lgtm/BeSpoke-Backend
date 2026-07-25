package com.BeSpoke.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/** Project detail wrapper: flat summary + milestones + invoices (admin-only, omitted otherwise). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProjectDetailDto(
        ProjectDto project,
        List<MilestoneDto> milestones,
        Long leadId,
        List<InvoiceDto> invoices
) {
}
