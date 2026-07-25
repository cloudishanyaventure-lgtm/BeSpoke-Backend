package com.BeSpoke.dto;

import com.BeSpoke.entity.LeadActivity;

import java.time.Instant;

public record ActivityDto(
        Long id,
        Long leadId,
        String leadName,
        String type,
        String body,
        String authorName,
        Instant createdAt
) {

    public static ActivityDto from(LeadActivity activity) {
        return new ActivityDto(
                activity.getId(),
                activity.getLead().getId(),
                activity.getLead().getContactName(),
                activity.getType().name(),
                activity.getBody(),
                activity.getAuthor() != null ? activity.getAuthor().getName() : "System",
                activity.getCreatedAt()
        );
    }
}
