package com.BeSpoke.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 2000) String description,
        @NotBlank @Pattern(regexp = "DESIGNS|FURNITURE|MATERIALS") String category,
        @Size(max = 60) String roomType,
        @NotNull @Positive BigDecimal price,
        @Size(max = 1000) String imageUrl
) {
}
