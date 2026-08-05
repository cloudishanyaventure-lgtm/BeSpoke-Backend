package com.BeSpoke.dto;

import com.BeSpoke.entity.User;

/** Public-safe view of a user. Never exposes the password hash. */
public record UserDto(Long id, String name, String email, String phone, String role, String city,
                      String title, Long companyId, String companyName, String companyType,
                      Boolean solo) {

    /** Without a staff profile to hand — `title` comes back null (customers have none). */
    public static UserDto from(User user) {
        return from(user, null);
    }

    /** `title` is the staff-profile designation the sidebar shows under the name (V3 §12). */
    public static UserDto from(User user, String title) {
        if (user == null) {
            return null;
        }
        return new UserDto(user.getId(), user.getName(), user.getEmail(), user.getPhone(),
                user.getRole().name(), user.getCity(), title,
                user.getCompany() != null ? user.getCompany().getId() : null,
                user.getCompany() != null ? user.getCompany().getName() : null,
                user.getCompany() != null ? user.getCompany().getType().name() : null,
                user.getCompany() != null ? user.getCompany().getSolo() : null);
    }
}
