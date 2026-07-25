package com.BeSpoke.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/** project is the flat summary created on WON (omitted when the lead has no project). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LeadDetailDto(
        LeadSummaryDto lead,
        RequirementFormDto form,
        List<ActivityDto> activities,
        List<QuoteDto> quotes,
        ProjectDto project
) {
}
