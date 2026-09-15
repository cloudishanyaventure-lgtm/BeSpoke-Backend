package com.BeSpoke.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;

/** lifetimeBilled / lifetimeCollected are admin-only and omitted for designers. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClientDto(
        Long userId,
        String name,
        String email,
        String phone,
        String city,
        Instant createdAt,
        long leadsCount,
        long projects,
        /** Their most recent lead — where the rest of the journey is worked. */
        Long leadId,
        String leadStatus,
        BigDecimal lifetimeBilled,
        BigDecimal lifetimeCollected
) {
}
