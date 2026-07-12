package com.BeSpoke.dto;

import com.BeSpoke.entity.ChatThread;

import java.time.Instant;

public record ChatThreadDto(
        Long id,
        Long leadId,
        UserDto customer,
        UserDto designer,
        Instant createdAt
) {

    public static ChatThreadDto from(ChatThread thread) {
        return new ChatThreadDto(
                thread.getId(),
                thread.getLead().getId(),
                UserDto.from(thread.getCustomer()),
                UserDto.from(thread.getDesigner()),
                thread.getCreatedAt()
        );
    }
}
