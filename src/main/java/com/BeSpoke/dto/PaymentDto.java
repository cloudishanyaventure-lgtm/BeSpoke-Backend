package com.BeSpoke.dto;

import com.BeSpoke.entity.InvoicePayment;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentDto(
        Long id,
        BigDecimal amount,
        String mode,
        String reference,
        LocalDate paidAt,
        String recordedByName
) {

    public static PaymentDto from(InvoicePayment payment) {
        return new PaymentDto(
                payment.getId(),
                payment.getAmount(),
                payment.getMode().name(),
                payment.getReference(),
                payment.getPaidAt(),
                payment.getRecordedBy() != null ? payment.getRecordedBy().getName() : null
        );
    }
}
