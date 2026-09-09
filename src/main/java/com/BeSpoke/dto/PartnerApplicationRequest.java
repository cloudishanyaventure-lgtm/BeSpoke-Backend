package com.BeSpoke.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Public partner sign-up form — five fields, and it creates an application, never an account. */
public record PartnerApplicationRequest(
        @NotBlank @Size(max = 255) String contactName,
        @NotBlank @Size(max = 255) String companyName,
        @NotBlank @Size(max = 120) String city,
        @NotBlank @Email @Size(max = 255) String contactEmail,
        @NotBlank @Size(min = 7, max = 30) String contactPhone
) {
}
