package com.BeSpoke.dto;

import com.BeSpoke.entity.LedgerPayment;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One line of the money register — received from a customer or made to a vendor. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LedgerPaymentDto(
        Long id,
        String direction,
        String partyName,
        Long customerId,
        Long vendorCompanyId,
        BigDecimal amount,
        String mode,
        String reference,
        LocalDate paidAt,
        String note,
        String recordedByName
) {
    public static LedgerPaymentDto from(LedgerPayment payment) {
        return new LedgerPaymentDto(
                payment.getId(),
                payment.getDirection().name(),
                payment.getPartyName(),
                payment.getCustomer() != null ? payment.getCustomer().getId() : null,
                payment.getVendorCompany() != null ? payment.getVendorCompany().getId() : null,
                payment.getAmount(),
                payment.getMode().name(),
                payment.getReference(),
                payment.getPaidAt(),
                payment.getNote(),
                payment.getRecordedBy() != null ? payment.getRecordedBy().getName() : null);
    }
}
