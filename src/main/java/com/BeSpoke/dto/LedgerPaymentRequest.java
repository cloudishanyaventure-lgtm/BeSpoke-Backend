package com.BeSpoke.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Money received from a customer, or paid to a vendor. Exactly one party is named:
 * customerId for RECEIVED, vendorCompanyId or a typed vendorName for MADE.
 */
public record LedgerPaymentRequest(
        @NotBlank @Pattern(regexp = "RECEIVED|MADE") String direction,
        Long customerId,
        Long vendorCompanyId,
        /** An off-platform supplier — used when vendorCompanyId is absent. */
        @Size(max = 255) String vendorName,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @NotBlank @Pattern(regexp = "BANK_TRANSFER|UPI|CASH|CHEQUE",
                message = "mode must be BANK_TRANSFER, UPI, CASH or CHEQUE") String mode,
        /** UTR / transaction ID / cheque number. Required for everything but cash. */
        @Size(max = 255) String reference,
        LocalDate paidAt,
        @Size(max = 500) String note
) {
}
