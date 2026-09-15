package com.BeSpoke.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Staff filling in the details the customer never gave — the phone an email enquiry
 * carries no trace of, the city, the property. Blank/absent fields are left alone;
 * whatever is set also lands on the customer's own profile once they have an account.
 */
public record UpdateLeadContactRequest(
        @NotBlank @Size(max = 255) String name,
        @Email @Size(max = 255) String email,
        @Size(max = 30) String phone,
        @Size(max = 120) String city,
        @Size(max = 60) String propertyType,
        @Size(max = 60) String budgetBand
) {
}
