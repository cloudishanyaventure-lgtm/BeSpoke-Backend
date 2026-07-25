package com.BeSpoke.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecordPaymentRequest(
        @NotNull @Positive BigDecimal amount,
        @NotBlank @Pattern(regexp = "UPI|NEFT|RTGS|CHEQUE|CASH",
                message = "mode must be UPI, NEFT, RTGS, CHEQUE or CASH") String mode,
        @Size(max = 255) String reference,
        LocalDate paidAt
) {
}
