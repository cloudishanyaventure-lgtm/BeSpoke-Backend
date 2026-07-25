package com.BeSpoke.dto;

import com.BeSpoke.entity.QuoteItem;

import java.math.BigDecimal;

public record QuoteItemDto(
        Long id,
        String category,
        String description,
        BigDecimal qty,
        BigDecimal rate,
        int gstPct,
        BigDecimal lineTotal
) {

    public static QuoteItemDto from(QuoteItem item) {
        BigDecimal net = item.getQty().multiply(item.getRate());
        BigDecimal lineTotal = net.add(net.multiply(BigDecimal.valueOf(item.getGstPct()))
                .divide(BigDecimal.valueOf(100)));
        return new QuoteItemDto(item.getId(), item.getCategory().name(), item.getDescription(),
                item.getQty(), item.getRate(), item.getGstPct(), lineTotal);
    }
}
