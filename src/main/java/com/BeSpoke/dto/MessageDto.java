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

    /**
     * {@code body} is the decrypted plaintext — the stored column is AES-GCM ciphertext,
     * and decryption stays in MessageService so the DTO needs no crypto dependency.
     */
    public static MessageDto from(Message message, String plainBody) {
        return new MessageDto(
                message.getId(),
                message.getLead().getId(),
                message.getSender().getId(),
                message.getSender().getName(),
                message.getSender().getRole().name(),
                plainBody,
                message.getCreatedAt(),
                message.getReadAt()
        );
    }
}
