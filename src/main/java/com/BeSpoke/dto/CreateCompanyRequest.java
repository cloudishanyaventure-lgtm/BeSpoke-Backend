package com.BeSpoke.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Platform admin onboards a company: profile + KYC + its DIRECTOR account, atomically. */
public record CreateCompanyRequest(
        @NotBlank @Size(max = 255) String name,
        // Legacy alias for headquartersCity; used only when that is absent.
        @Size(max = 120) String city,
        @Size(max = 120) String headquartersCity,
        List<String> operationalCities,
        @Size(max = 30) String phone,
        @Email @Size(max = 255) String email,
        @Size(max = 2000) String about,
        @Size(max = 20) String accentColor,
        @Pattern(regexp = "DESIGN|VENDOR", message = "type must be DESIGN or VENDOR") String type,
        Boolean solo,
        @Size(max = 30) String gstin,
        @Size(max = 30) String pan,
        @Size(max = 30) String cin,
        @Size(max = 255) String registeredName,
        @Size(max = 500) String registeredAddress,
        List<String> kycDocUrls,
        List<String> enabledRoles,
        @NotBlank @Size(max = 255) String directorName,
        @NotBlank @Email String directorEmail,
        @Size(max = 30) String directorPhone,
        @NotBlank @Size(min = 6, max = 100) String directorPassword
) {
}
