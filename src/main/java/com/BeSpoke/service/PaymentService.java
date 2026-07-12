package com.BeSpoke.service;

import java.math.BigDecimal;

/**
 * Payment provider abstraction. The production implementation is intended to be
 * Razorpay (order + capture + route/transfer to the designer's account);
 * {@link MockPaymentProvider} is used for local development.
 */
public interface PaymentService {

    PaymentResult charge(BigDecimal amount, String currency, String description);

    String providerName();

    record PaymentResult(boolean success, String providerRef, String failureReason) {

        public static PaymentResult success(String providerRef) {
            return new PaymentResult(true, providerRef, null);
        }

        public static PaymentResult failure(String reason) {
            return new PaymentResult(false, null, reason);
        }
    }
}
