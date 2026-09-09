package com.BeSpoke.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * The approval decision — and the onboarding form. The five-field public form never asked
 * what they are, so the admin classifies them here: studio, solo or vendor, and for a
 * vendor what they actually supply (glass, electricals, …). GST identity and both
 * addresses are captured at the same time, while a human is already looking at them.
 * The password is the director's first one, emailed to them.
 */
public record ApprovePartnerRequest(
        @NotBlank @Size(min = 8, max = 72) String password,
        @Pattern(regexp = "DESIGN|VENDOR", message = "type must be DESIGN or VENDOR") String type,
        Boolean solo,
        /** Vendor trades — ignored for a DESIGN company. */
        List<String> vendorCategories,
        @Size(max = 30) String gstin,
        @Size(max = 30) String pan,
        @Size(max = 30) String cin,
        @Size(max = 255) String registeredName,
        @Size(max = 500) String officeAddress,
        /** The GST address. Ignored when gstSameAsOffice is true — officeAddress is copied. */
        @Size(max = 500) String gstAddress,
        Boolean gstSameAsOffice,
        @Size(max = 500) String note
) {
}
