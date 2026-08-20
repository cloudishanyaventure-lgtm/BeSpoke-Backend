package com.BeSpoke.dto;

import com.BeSpoke.entity.Company;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Year;
import java.util.List;

/**
 * Public directory card / profile for an onboarded company (design studio or vendor).
 * `city` is the headquarters. `designers` is filled on the profile endpoint only.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PublicStudioDto(Long id, String name, String slug, String city,
                              List<String> operationalCities,
                              String about, String logoUrl, String coverUrl,
                              String accentColor, String type, Boolean solo,
                              Integer foundedYear, Integer yearsExperience,
                              List<String> styles, List<String> portfolioUrls,
                              String leadName, String leadTitle, String leadAvatarUrl,
                              Integer teamSize,
                              List<PublicDesignerDto> designers) {

    /** Card view — no team roster. */
    public static PublicStudioDto from(Company company) {
        return from(company, null, null, null, null, null);
    }

    public static PublicStudioDto from(Company company, String leadName, String leadTitle,
                                       String leadAvatarUrl, Integer teamSize,
                                       List<PublicDesignerDto> designers) {
        Integer founded = company.getFoundedYear();
        return new PublicStudioDto(company.getId(), company.getName(), company.getSlug(),
                company.getHeadquartersCity(), List.copyOf(company.getOperationalCities()),
                company.getAbout(), company.getLogoUrl(), company.getCoverUrl(),
                company.getAccentColor(), company.getType().name(), company.getSolo(),
                founded, founded == null ? null : Math.max(0, Year.now().getValue() - founded),
                List.copyOf(company.getStyles()), List.copyOf(company.getPortfolioUrls()),
                leadName, leadTitle, leadAvatarUrl, teamSize, designers);
    }
}
