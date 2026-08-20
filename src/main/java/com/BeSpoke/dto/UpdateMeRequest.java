package com.BeSpoke.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

/** All fields optional - only non-null values are applied. */
public record UpdateMeRequest(
        @Size(max = 255) String name,
        @Size(max = 30) String phone,
        @Size(max = 120) String city,
        // Public designer card (staff only; ignored for customers with no staff profile).
        @Size(max = 1000) String avatarUrl,
        @Size(max = 2000) String bio,
        @Min(0) @Max(70) Integer yearsExperience,
        List<String> styles
) {
}
