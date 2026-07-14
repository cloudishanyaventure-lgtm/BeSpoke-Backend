package com.BeSpoke.dto;

import java.math.BigDecimal;

public record DesignerStatsDto(
        long profileViews,
        long pendingLeads,
        long activeProjects,
        long completedProjects,
        BigDecimal totalEarnings,
        Double avgRating,
        long reviewCount
) {
}
