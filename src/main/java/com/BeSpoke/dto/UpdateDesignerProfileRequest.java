package com.BeSpoke.dto;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** All fields optional - only non-null values are applied. */
public record UpdateDesignerProfileRequest(
        @Size(max = 2000) String bio,
        @Size(max = 255) String specialties,
        @Size(max = 255) String city,
        BigDecimal startingPrice,
        List<@Size(max = 1000) String> portfolioImageUrls
) {
}
