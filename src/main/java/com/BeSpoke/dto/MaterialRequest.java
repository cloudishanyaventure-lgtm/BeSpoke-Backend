package com.BeSpoke.dto;

import com.BeSpoke.entity.MaterialTier;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/** Admin create/update payload. Null = leave as-is on update, empty = clear. */
public record MaterialRequest(
        @NotBlank String name,
        @NotBlank String categorySlug,
        String slug,
        String blurb,
        String description,
        String specs,
        String applications,
        String finish,
        String colour,
        String texture,
        String thickness,
        String sheetSize,
        String standard,
        String warranty,
        String installation,
        MaterialTier tier,
        BigDecimal priceMin,
        BigDecimal priceMax,
        String priceUnit,
        Boolean waterResistant,
        Boolean fireResistant,
        Boolean scratchResistant,
        String usage,
        String imageUrl,
        String imageKeyword,
        Integer sortOrder,
        Boolean active
) {
}
