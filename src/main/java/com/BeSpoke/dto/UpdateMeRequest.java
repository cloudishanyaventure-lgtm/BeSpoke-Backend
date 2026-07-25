package com.BeSpoke.dto;

import jakarta.validation.constraints.Size;

/** All fields optional - only non-null values are applied. */
public record UpdateMeRequest(
        @Size(max = 255) String name,
        @Size(max = 30) String phone,
        @Size(max = 120) String city
) {
}
