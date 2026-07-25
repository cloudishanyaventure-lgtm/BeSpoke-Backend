package com.BeSpoke.dto;

import com.BeSpoke.entity.User;

/** Minimal user reference for embedding in other DTOs. */
public record UserRefDto(Long id, String name) {

    public static UserRefDto from(User user) {
        if (user == null) {
            return null;
        }
        return new UserRefDto(user.getId(), user.getName());
    }
}
