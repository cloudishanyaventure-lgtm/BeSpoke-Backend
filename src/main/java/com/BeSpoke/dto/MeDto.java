package com.BeSpoke.dto;

import com.BeSpoke.entity.DesignerProfile;
import com.BeSpoke.entity.User;

import java.math.BigDecimal;
import java.util.List;

/** The authenticated user's own profile. Never exposes the password hash. */
public record MeDto(
        Long id,
        String name,
        String email,
        String role,
        String phone,
        String city,
        String avatarUrl,
        DesignerProfileDto designerProfile
) {

    public record DesignerProfileDto(
            String bio,
            String specialties,
            String city,
            Double rating,
            BigDecimal startingPrice,
            List<String> portfolioImageUrls,
            long viewCount,
            long reviewCount
    ) {
    }

    public static MeDto from(User user, DesignerProfile profileOrNull, long reviewCount) {
        DesignerProfileDto profileDto = null;
        if (profileOrNull != null) {
            profileDto = new DesignerProfileDto(
                    profileOrNull.getBio(),
                    profileOrNull.getSpecialties(),
                    profileOrNull.getCity(),
                    profileOrNull.getRating(),
                    profileOrNull.getStartingPrice(),
                    List.copyOf(profileOrNull.getPortfolioImageUrls()),
                    profileOrNull.getViewCount(),
                    reviewCount
            );
        }
        return new MeDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getPhone(),
                user.getCity(),
                user.getAvatarUrl(),
                profileDto
        );
    }
}
