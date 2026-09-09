package com.BeSpoke.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PlaceOrderRequest(
        @NotEmpty @Size(max = 100) @Valid List<Item> items,
        @NotBlank @Size(max = 500) String shippingAddress,
        @NotBlank @Size(min = 7, max = 30) String phone,
        @jakarta.validation.constraints.Pattern(regexp = "[a-zA-Z0-9-]{16,80}") String checkoutKey,
        @jakarta.validation.constraints.Positive java.math.BigDecimal expectedTotal
) {
    public PlaceOrderRequest(List<Item> items, String shippingAddress, String phone) {
        this(items, shippingAddress, phone, null, null);
    }

    public record Item(@NotNull Long productId, @Min(1) @jakarta.validation.constraints.Max(999) int qty) {
    }
}
