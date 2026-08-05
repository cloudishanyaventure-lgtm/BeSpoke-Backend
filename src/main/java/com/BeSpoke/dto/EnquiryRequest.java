package com.BeSpoke.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Public enquiry form - creates a lead without a user account. */
public record EnquiryRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(min = 7, max = 30) String phone,
        @NotBlank @Email String email,
        @NotBlank @Size(max = 120) String city,
        @Size(max = 60) String propertyType,
        @Size(max = 60) String budgetBand,
        @Size(max = 2000) String message,
        Long companyId
) {
}
