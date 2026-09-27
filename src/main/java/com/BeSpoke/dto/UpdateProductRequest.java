package com.BeSpoke.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Partial update — null fields are left unchanged. */
public record UpdateProductRequest(
        @Size(max = 200) String name,
        @Size(max = 2000) String description,
        @Pattern(regexp = "DESIGNS|FURNITURE|MATERIALS") String category,
        @Size(max = 60) String roomType,
        @Size(max = 120) String shopCategory,
        @Size(max = 120) String shopSubCategory,
        @Positive BigDecimal price,
        /** 0 clears the list price; any other value sets it. */
        @jakarta.validation.constraints.PositiveOrZero BigDecimal mrp,
        @Size(max = 60) String material,
        @Size(max = 1000) String imageUrl,
        Boolean active,
        @jakarta.validation.Valid ProductSpatialSpec spatial
) {
}
