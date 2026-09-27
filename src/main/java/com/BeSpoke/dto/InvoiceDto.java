package com.BeSpoke.dto;

import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.Invoice;
import com.BeSpoke.entity.InvoicePayment;
import com.BeSpoke.entity.InvoiceStatus;
import com.BeSpoke.entity.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * status is derived for presentation: DRAFT and SENT come from the entity;
 * PAID once payments cover the total (also persisted); PARTIALLY_PAID and
 * OVERDUE are computed from payments and dueDate.
 */
public record InvoiceDto(
        Long id,
        Long projectId,
        String projectName,
        String clientName,
        Long milestoneId,
        String milestoneTitle,
        String number,
        String title,
        BigDecimal amount,
        int gstPct,
        BigDecimal total,
        BigDecimal paid,
        BigDecimal balance,
        LocalDate dueDate,
        String status,
        Instant createdAt,
        List<PaymentDto> payments,
        Party seller,
        Party buyer
) {

    /** A billing party on the invoice — the studio (seller) or the customer (buyer). */
    public record Party(String name, String gstin, String pan, String address,
                        String city, String phone, String email) {
    }

    public static InvoiceDto from(Invoice invoice, List<InvoicePayment> payments) {
        BigDecimal total = totalOf(invoice);
        BigDecimal paid = payments.stream()
                .map(InvoicePayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new InvoiceDto(
                invoice.getId(),
                invoice.getProject().getId(),
                invoice.getProject().getName(),
                invoice.getProject().getClient() != null
                        ? invoice.getProject().getClient().getName()
                        : invoice.getProject().getLead().getContactName(),
                invoice.getMilestone() != null ? invoice.getMilestone().getId() : null,
                invoice.getMilestone() != null ? invoice.getMilestone().getTitle() : null,
                invoice.getNumber(),
                invoice.getTitle(),
                invoice.getAmount(),
                invoice.getGstPct(),
                total,
                paid,
                total.subtract(paid),
                invoice.getDueDate(),
                deriveStatus(invoice, total, paid),
                invoice.getCreatedAt(),
                payments.stream().map(PaymentDto::from).toList(),
                sellerOf(invoice),
                buyerOf(invoice)
        );
    }

    private static Party sellerOf(Invoice invoice) {
        Company co = invoice.getProject().getLead() == null
                ? null : invoice.getProject().getLead().getCompany();
        if (co == null) {
            return null;
        }
        return new Party(
                co.getRegisteredName() != null ? co.getRegisteredName() : co.getName(),
                co.getGstin(), co.getPan(),
                co.getOfficeAddress() != null ? co.getOfficeAddress() : co.getRegisteredAddress(),
                co.getHeadquartersCity() != null ? co.getHeadquartersCity() : co.getCity(),
                co.getPhone(), co.getEmail());
    }

    private static Party buyerOf(Invoice invoice) {
        User client = invoice.getProject().getClient();
        if (client != null) {
            return new Party(client.getName(), null, null, null,
                    client.getCity(), client.getPhone(), client.getEmail());
        }
        String name = invoice.getProject().getLead() == null
                ? null : invoice.getProject().getLead().getContactName();
        return new Party(name, null, null, null, null, null, null);
    }

    public static BigDecimal totalOf(Invoice invoice) {
        return invoice.getAmount()
                .add(invoice.getAmount()
                        .multiply(BigDecimal.valueOf(invoice.getGstPct()))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
    }

    private static String deriveStatus(Invoice invoice, BigDecimal total, BigDecimal paid) {
        if (invoice.getStatus() == InvoiceStatus.PAID || paid.compareTo(total) >= 0) {
            return "PAID";
        }
        if (invoice.getStatus() == InvoiceStatus.DRAFT) {
            return "DRAFT";
        }
        if (invoice.getDueDate() != null && invoice.getDueDate().isBefore(LocalDate.now())) {
            return "OVERDUE";
        }
        if (paid.compareTo(BigDecimal.ZERO) > 0) {
            return "PARTIALLY_PAID";
        }
        return "SENT";
    }
}
