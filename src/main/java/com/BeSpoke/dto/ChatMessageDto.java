package com.BeSpoke.dto;

import com.BeSpoke.entity.ChatMessage;

import java.time.Instant;

public record ChatMessageDto(
        Long id,
        Long threadId,
        UserDto sender,
        String content,
        Instant sentAt
) {

    public static ChatMessageDto from(ChatMessage message) {
        return new ChatMessageDto(
                message.getId(),
                message.getThread().getId(),
                UserDto.from(message.getSender()),
                message.getContent(),
                message.getSentAt()
        );
    }
}
