package com.BeSpoke.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CheckoutRequest(
        @NotEmpty @Valid List<CartItemRequest> items,
        Long designerId,
        @NotBlank String address
) {
}
