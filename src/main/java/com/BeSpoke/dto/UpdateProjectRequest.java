package com.BeSpoke.dto;

import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/** All fields optional - only non-null values are applied. budget is admin-only. */
public record UpdateProjectRequest(
        String stage,
        String health,
        @PositiveOrZero BigDecimal budget,
        LocalDate startDate,
        LocalDate targetDate
) {
}
