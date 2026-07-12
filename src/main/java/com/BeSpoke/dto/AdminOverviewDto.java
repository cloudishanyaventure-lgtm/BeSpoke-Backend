package com.BeSpoke.dto;

import java.util.Map;

public record AdminOverviewDto(
        long totalCustomers,
        long totalDesigners,
        long totalServices,
        long totalOrders,
        Map<String, Long> leadsByStatus
) {
}
