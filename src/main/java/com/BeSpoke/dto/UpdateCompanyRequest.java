package com.BeSpoke.dto;

import jakarta.validation.constraints.Email;
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
        @Size(max = 20) String accentColor,
        Boolean active
) {
}
