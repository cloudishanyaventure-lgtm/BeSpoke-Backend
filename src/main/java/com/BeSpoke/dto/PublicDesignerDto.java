package com.BeSpoke.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/** Public card / profile for a designer: active staff with a completed public profile. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PublicDesignerDto(Long id, String name, String title, String city,
                                String avatarUrl, String bio, Integer yearsExperience,
                                List<String> styles, String role,
                                Long companyId, String companyName, String companySlug,
                                List<String> portfolioUrls) {
}
