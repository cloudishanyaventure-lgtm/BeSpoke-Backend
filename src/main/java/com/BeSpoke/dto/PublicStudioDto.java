package com.BeSpoke.dto;

import com.BeSpoke.entity.Company;

import java.util.List;

/** Public directory card for an active studio. `city` is the headquarters. */
public record PublicStudioDto(Long id, String name, String slug, String city,
                              List<String> operationalCities,
                              String about, String logoUrl, String accentColor) {

    public static PublicStudioDto from(Company company) {
        return new PublicStudioDto(company.getId(), company.getName(), company.getSlug(),
                company.getHeadquartersCity(), List.copyOf(company.getOperationalCities()),
                company.getAbout(), company.getLogoUrl(), company.getAccentColor());
    }
}
