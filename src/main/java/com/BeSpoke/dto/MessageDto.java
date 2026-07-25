package com.BeSpoke.dto;

import com.BeSpoke.entity.Message;

import java.time.Instant;

public record MessageDto(
        Long id,
        Long leadId,
        Long senderId,
        String senderName,
        String senderRole,
        String body,
        Instant createdAt,
        Instant readAt
) {

    public static MessageDto from(Message message) {
        return new MessageDto(
                message.getId(),
                message.getLead().getId(),
                message.getSender().getId(),
                message.getSender().getName(),
                message.getSender().getRole().name(),
                message.getBody(),
                message.getCreatedAt(),
                message.getReadAt()
        );
    }
}
