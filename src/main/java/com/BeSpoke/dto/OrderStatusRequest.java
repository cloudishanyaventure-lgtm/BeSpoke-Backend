package com.BeSpoke.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OrderStatusRequest(
        @NotBlank @Pattern(regexp = "NEW|CONFIRMED|SHIPPED|DELIVERED|CANCELLED") String status
) {
}
