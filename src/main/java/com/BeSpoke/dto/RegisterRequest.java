package com.BeSpoke.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Signing up IS the lead: registration always creates a CUSTOMER plus a NEW_INQUIRY lead.
 * `password` is ignored — the server generates one and emails it (V3 §6). `companyId` is
 * a studio *preference*; the lead still starts in the BeSpoke pool.
 */
public record RegisterRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email String email,
        // Ten digits, nothing else: the signup box takes a mobile number, and the OTP
        // goes to it on WhatsApp. Clients strip spaces and punctuation before posting.
        @NotBlank @Pattern(regexp = "\\d{10}", message = "Enter a 10-digit mobile number")
        String phone,
        String password,
        @NotBlank @Size(max = 120) String city,
        @NotBlank(message = "Pick a property type") @Size(max = 60) String propertyType,
        @Size(max = 60) String budgetBand,
        Long companyId
) {
}
