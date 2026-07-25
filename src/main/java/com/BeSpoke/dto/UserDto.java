package com.BeSpoke.dto;

import com.BeSpoke.entity.User;

/** Public-safe view of a user. Never exposes the password hash. */
public record UserDto(Long id, String name, String email, String phone, String role, String city) {

    public static UserDto from(User user) {
        if (user == null) {
            return null;
        }
        return new UserDto(user.getId(), user.getName(), user.getEmail(), user.getPhone(),
                user.getRole().name(), user.getCity());
    }
}
