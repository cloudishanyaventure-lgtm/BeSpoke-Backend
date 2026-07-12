package com.BeSpoke.dto;

import com.BeSpoke.entity.CartItem;

import java.math.BigDecimal;

public record CartItemDto(
        Long id,
        ServiceDto service,
        int quantity,
        BigDecimal lineTotal
) {

    public static CartItemDto from(CartItem item) {
        BigDecimal lineTotal = item.getService().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartItemDto(item.getId(), ServiceDto.from(item.getService()), item.getQuantity(), lineTotal);
    }
}
