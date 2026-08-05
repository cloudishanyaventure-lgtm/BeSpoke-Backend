package com.BeSpoke.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PlaceOrderRequest(
        @NotEmpty @Valid List<Item> items,
        @NotBlank @Size(max = 500) String shippingAddress,
        @NotBlank @Size(min = 7, max = 30) String phone
) {

    public record Item(@NotNull Long productId, @Min(1) int qty) {
    }
}
