package com.BeSpoke.dto;

import com.BeSpoke.entity.User;

import java.time.Instant;

public record AdminUserDto(
        Long id,
        String name,
        String email,
        String role,
        String phone,
        String city,
        String avatarUrl,
        boolean active,
        Instant createdAt
) {

    public static AdminUserDto from(User user) {
        return new AdminUserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getPhone(),
                user.getCity(),
                user.getAvatarUrl(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
