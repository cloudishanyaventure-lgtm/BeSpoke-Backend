package com.BeSpoke.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** All fields optional - only non-null values are applied. budget is admin-only. */
public record UpdateProjectRequest(
        String stage,
        String health,
        BigDecimal budget,
        LocalDate startDate,
        LocalDate targetDate
) {
}
