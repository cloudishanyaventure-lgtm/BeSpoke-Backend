package com.BeSpoke.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mock payment provider that always succeeds. Replace with a Razorpay
 * implementation of {@link PaymentService} for production.
 */
@Service
public class MockPaymentProvider implements PaymentService {

    @Override
    public PaymentResult charge(BigDecimal amount, String currency, String description) {
        String ref = "mock_pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return PaymentResult.success(ref);
    }

    @Override
    public String providerName() {
        return "MOCK";
    }
}
