package com.BeSpoke.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

/**
 * The customer's read-only journey: funnel position, designer, project, counts,
 * plus the V2 transfer state (POOL → TRANSFERRED → ACCEPTED) and drawing pipeline.
 */
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
        long unreadMessages,
        CompanyCardDto company,
        String transferState,
        // POOL → TRANSFERRED → ACCEPTED with timestamps, so the customer always sees
        // where their request sits — including after acceptance (V3 §7).
        List<RouteEventDto> routeHistory,
        DrawingCountsDto drawingCounts
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

    public record RouteEventDto(String state, Instant at, String companyName) {
    }

    public record DesignerCardDto(String name, String title, String phone) {
    }

    /** Identity of the studio working the project; null while the lead is in the pool. */
    public record CompanyCardDto(String name, String logoUrl, String city) {
    }

    public record DrawingCountsDto(
            long wip,
            long pending,
            long approved,
            @JsonProperty("final") long finalCount
    ) {
    }
}
