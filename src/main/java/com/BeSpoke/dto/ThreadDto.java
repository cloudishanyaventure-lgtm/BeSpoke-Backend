package com.BeSpoke.dto;

import java.time.Instant;

/** A message thread == a lead. */
public record ThreadDto(
        Long leadId,
        String contactName,
        String leadStatus,
        UserRefDto assignedDesigner,
        String lastMessage,
        String lastMessageSender,
        Instant lastAt,
        long unread
) {
}
