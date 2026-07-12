package com.BeSpoke.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemRequest(
        @NotNull Long serviceId,
        @Min(1) int quantity
) {
}
