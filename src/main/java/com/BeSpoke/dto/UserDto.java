package com.BeSpoke.dto;

import com.BeSpoke.entity.StaffProfile;
import com.BeSpoke.entity.User;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/** Public-safe view of a user. Never exposes the password hash. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserDto(Long id, String name, String email, String phone, String role, String city,
                      String title, Long companyId, String companyName, String companyType,
                      Boolean solo, String avatarUrl, String bio, Integer yearsExperience,
                      List<String> styles, List<String> missingProfile) {

    /** Without a staff profile to hand — `title` comes back null (customers have none). */
    public static UserDto from(User user) {
        return from(user, (String) null);
    }

    /** `title` is the staff-profile designation the sidebar shows under the name (V3 §12). */
    public static UserDto from(User user, String title) {
        return build(user, title, null, null, null, null);
    }

    /** Full view for "my public profile": the designer card fields plus what is still blank. */
    public static UserDto from(User user, StaffProfile profile, List<String> missingProfile) {
        if (profile == null) {
            return build(user, null, null, null, List.of(), missingProfile);
        }
        return build(user, profile.getTitle(), profile.getBio(), profile.getYearsExperience(),
                List.copyOf(profile.getStyles()), missingProfile);
    }

    private static UserDto build(User user, String title, String bio, Integer yearsExperience,
                                 List<String> styles, List<String> missingProfile) {
        if (user == null) {
            return null;
        }
        return new UserDto(user.getId(), user.getName(), user.getEmail(), user.getPhone(),
                user.getRole().name(), user.getCity(), title,
                user.getCompany() != null ? user.getCompany().getId() : null,
                user.getCompany() != null ? user.getCompany().getName() : null,
                user.getCompany() != null ? user.getCompany().getType().name() : null,
                user.getCompany() != null ? user.getCompany().getSolo() : null,
                user.getAvatarUrl(), bio, yearsExperience, styles, missingProfile);
    }
}
