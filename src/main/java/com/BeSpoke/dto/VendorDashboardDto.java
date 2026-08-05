package com.BeSpoke.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** revenue (delivered totals) is present only for seesFinance roles. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VendorDashboardDto(
        long productCount,
        Map<String, Long> ordersByStatus,
        List<ShopOrderDto> recentOrders,
        BigDecimal revenue
) {
}
