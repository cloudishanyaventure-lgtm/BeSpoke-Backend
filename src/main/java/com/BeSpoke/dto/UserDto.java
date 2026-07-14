package com.BeSpoke.dto;

import com.BeSpoke.entity.User;

public record UserDto(Long id, String name, String email, String role, String avatarUrl) {

    public static UserDto from(User user) {
        if (user == null) {
            return null;
        }
        return new UserDto(user.getId(), user.getName(), user.getEmail(), user.getRole().name(),
                user.getAvatarUrl());
    }
}
