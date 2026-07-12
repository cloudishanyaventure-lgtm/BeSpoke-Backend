package com.BeSpoke.dto;

import com.BeSpoke.entity.DesignerProfile;

import java.math.BigDecimal;
import java.util.List;

public record DesignerDto(
        Long id,
        Long userId,
        String name,
        String bio,
        String specialties,
        String city,
        Double rating,
        BigDecimal startingPrice,
        List<String> portfolioImageUrls
) {

    public static DesignerDto from(DesignerProfile profile) {
        return new DesignerDto(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getName(),
                profile.getBio(),
                profile.getSpecialties(),
                profile.getCity(),
                profile.getRating(),
                profile.getStartingPrice(),
                List.copyOf(profile.getPortfolioImageUrls())
        );
    }
}
