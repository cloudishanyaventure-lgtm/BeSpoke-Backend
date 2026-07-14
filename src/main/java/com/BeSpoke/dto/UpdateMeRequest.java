package com.BeSpoke.dto;

import jakarta.validation.constraints.Size;

/** All fields optional - only non-null values are applied. */
public record UpdateMeRequest(
        @Size(max = 255) String name,
        @Size(max = 30) String phone,
        @Size(max = 120) String city,
        @Size(max = 1000) String avatarUrl,
        @Size(min = 6, max = 100) String password
) {
}
