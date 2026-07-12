package com.BeSpoke.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnquiryRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 7, max = 20) String phone,
        @NotBlank @Size(max = 3000) String message,
        String category
) {
}
