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
        List<String> portfolioImageUrls,
        String avatarUrl,
        long viewCount,
        long reviewCount
) {

    /**
     * @param liveAvgRating live average of reviews, or null when the designer has no reviews
     *                      (falls back to the stored rating).
     */
    public static DesignerDto from(DesignerProfile profile, long reviewCount, Double liveAvgRating) {
        return new DesignerDto(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getName(),
                profile.getBio(),
                profile.getSpecialties(),
                profile.getCity(),
                liveAvgRating != null ? liveAvgRating : profile.getRating(),
                profile.getStartingPrice(),
                List.copyOf(profile.getPortfolioImageUrls()),
                profile.getUser().getAvatarUrl(),
                profile.getViewCount(),
                reviewCount
        );
    }
}
