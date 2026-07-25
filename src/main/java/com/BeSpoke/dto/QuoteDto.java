package com.BeSpoke.dto;

import com.BeSpoke.entity.Quote;
import com.BeSpoke.entity.QuoteItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record QuoteDto(
        Long id,
        Long leadId,
        String contactName,
        int version,
        String title,
        String status,
        LocalDate validUntil,
        Instant sentAt,
        Instant decidedAt,
        String customerComment,
        Instant createdAt,
        BigDecimal subtotal,
        BigDecimal gst,
        BigDecimal total,
        List<QuoteItemDto> items
) {

    public static QuoteDto from(Quote quote) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal gst = BigDecimal.ZERO;
        for (QuoteItem item : quote.getItems()) {
            BigDecimal net = item.getQty().multiply(item.getRate());
            subtotal = subtotal.add(net);
            gst = gst.add(net.multiply(BigDecimal.valueOf(item.getGstPct()))
                    .divide(BigDecimal.valueOf(100)));
        }
        return new QuoteDto(
                quote.getId(),
                quote.getLead().getId(),
                quote.getLead().getContactName(),
                quote.getVersion(),
                quote.getTitle(),
                quote.getStatus().name(),
                quote.getValidUntil(),
                quote.getSentAt(),
                quote.getDecidedAt(),
                quote.getCustomerComment(),
                quote.getCreatedAt(),
                subtotal,
                gst,
                subtotal.add(gst),
                quote.getItems().stream().map(QuoteItemDto::from).toList()
        );
    }
}
