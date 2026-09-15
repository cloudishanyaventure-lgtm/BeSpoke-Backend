package com.BeSpoke.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Null fields are left unchanged. `active` is honoured for platform admins only. */
public record UpdateCompanyRequest(
        @Size(max = 255) String name,
        // Legacy alias for headquartersCity; used only when that is absent.
        @Size(max = 120) String city,
        @Size(max = 120) String headquartersCity,
        List<String> operationalCities,
        @Size(max = 30) String phone,
        @Email @Size(max = 255) String email,
        @Size(max = 2000) String about,
        @Size(max = 1000) String logoUrl,
        @Size(max = 1000) String coverUrl,
        @Min(1900) @Max(2100) Integer foundedYear,
        List<String> styles,
        List<String> portfolioUrls,
        /** Photo URL → the section it belongs under. Replaces the whole map. */
        java.util.Map<String, String> portfolioSections,
        @Size(max = 20) String accentColor,
        List<String> vendorCategories,
        @Size(max = 30) String gstin,
        @Size(max = 30) String pan,
        @Size(max = 30) String cin,
        @Size(max = 255) String registeredName,
        @Size(max = 500) String officeAddress,
        /** The GST address. Ignored when gstSameAsOffice is true — officeAddress is copied. */
        @Size(max = 500) String gstAddress,
        Boolean gstSameAsOffice,
        /** Who fronts the public Designers tab; must be active staff of this company. */
        Long featuredDesignerId,
        Boolean active
) {
}
