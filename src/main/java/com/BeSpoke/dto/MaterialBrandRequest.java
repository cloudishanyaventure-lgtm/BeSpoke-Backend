package com.BeSpoke.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record MaterialBrandRequest(
        @NotBlank String name,
        String slug,
        String note,
        String imageUrl,
        List<String> categorySlugs,
        Boolean active
) {
}
