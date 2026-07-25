package com.BeSpoke.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/** The customer's read-only journey: funnel position, designer, project, counts. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record JourneyDto(
        LeadJourneyDto lead,
        DesignerCardDto designer,
        ProjectDto project,
        boolean hasForm,
        String formStatus,
        long quoteCount,
        long pendingQuoteCount,
        long invoiceCount,
        long unreadMessages
) {

    public record LeadJourneyDto(
            Long id,
            String status,
            Instant createdAt,
            List<StageEventDto> stageHistory
    ) {
    }

    public record StageEventDto(String stage, Instant at) {
    }

    public record DesignerCardDto(String name, String title, String phone) {
    }
}
