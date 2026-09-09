package com.BeSpoke.dto;
import com.BeSpoke.entity.AppNotification;
import java.time.Instant;
public record NotificationDto(Long id, String title, String body, String link, Instant createdAt, Instant readAt) {
    public static NotificationDto from(AppNotification n) { return new NotificationDto(n.getId(), n.getTitle(), n.getBody(), n.getLink(), n.getCreatedAt(), n.getReadAt()); }
}
