package com.BeSpoke.dto;

import jakarta.validation.constraints.NotBlank;

public record MaterialCategoryRequest(
        @NotBlank String name,
        String slug,
        String tagline,
        String description,
        String imageUrl,
        Integer sortOrder,
        Boolean active
) {
}
