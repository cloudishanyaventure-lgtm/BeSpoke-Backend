package com.BeSpoke.dto;

import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.Role;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/** Company profile. members/openLeads/directorName are filled for admin lists only. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CompanyDto(
        Long id,
        String name,
        String slug,
        String city,
        String headquartersCity,
        List<String> operationalCities,
        String phone,
        String email,
        String about,
        String logoUrl,
        String coverUrl,
        Integer foundedYear,
        List<String> styles,
        List<String> portfolioUrls,
        String accentColor,
        boolean active,
        String type,
        Boolean solo,
        String kycStatus,
        String gstin,
        String pan,
        String cin,
        String registeredName,
        String registeredAddress,
        List<String> kycDocUrls,
        List<String> enabledRoles,
        Instant createdAt,
        // Human names of the KYC fields still blank — empty means Verify is allowed (V3 §2).
        List<String> missingKyc,
        // Human names of the public-profile fields still blank — empty means it is listed.
        List<String> missingProfile,
        Long members,
        Long openLeads,
        String directorName
) {
    public static CompanyDto from(Company company, List<String> missingKyc,
                                  List<String> missingProfile) {
        return from(company, missingKyc, missingProfile, null, null, null);
    }

    public static CompanyDto from(Company company, List<String> missingKyc,
                                  List<String> missingProfile,
                                  Long members, Long openLeads, String directorName) {
        return new CompanyDto(company.getId(), company.getName(), company.getSlug(),
                company.getCity(), company.getHeadquartersCity(),
                List.copyOf(company.getOperationalCities()),
                company.getPhone(), company.getEmail(), company.getAbout(),
                company.getLogoUrl(), company.getCoverUrl(), company.getFoundedYear(),
                List.copyOf(company.getStyles()), List.copyOf(company.getPortfolioUrls()),
                company.getAccentColor(), company.isActive(),
                company.getType().name(), company.getSolo(), company.getKycStatus().name(),
                company.getGstin(), company.getPan(), company.getCin(),
                company.getRegisteredName(), company.getRegisteredAddress(),
                List.copyOf(company.getKycDocUrls()),
                company.effectiveEnabledRoles().stream().map(Role::name).toList(),
                company.getCreatedAt(), missingKyc, missingProfile,
                members, openLeads, directorName);
    }
}
