package com.BeSpoke.dto;

import com.BeSpoke.entity.TeamMessage;

import java.time.Instant;

/** One internal chat message. {@code body} is plaintext — decrypted in TeamChatService. */
public record TeamMessageDto(
        Long id,
        Long senderId,
        String senderName,
        String senderRole,
        Long recipientId,
        String body,
        Instant createdAt,
        Instant readAt
) {

    public static TeamMessageDto from(TeamMessage message, String plainBody) {
        return new TeamMessageDto(
                message.getId(),
                message.getSender().getId(),
                message.getSender().getName(),
                message.getSender().getRole().name(),
                message.getRecipient().getId(),
                plainBody,
                message.getCreatedAt(),
                message.getReadAt()
        );
    }
}
