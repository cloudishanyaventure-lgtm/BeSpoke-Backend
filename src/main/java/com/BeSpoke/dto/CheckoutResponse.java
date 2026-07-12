package com.BeSpoke.dto;

import java.math.BigDecimal;

public record CheckoutResponse(
        Long orderId,
        Long paymentId,
        Long leadId,
        String leadStatus,
        BigDecimal totalAmount,
        String paymentProviderRef
) {
}
