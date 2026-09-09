package com.BeSpoke.service;
import com.BeSpoke.entity.*;
import com.BeSpoke.dto.NotificationDto;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.AppNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
@Service
@Transactional(readOnly = true)
public class NotificationService {
    private final AppNotificationRepository notifications;
    public NotificationService(AppNotificationRepository notifications) { this.notifications = notifications; }
    @Transactional
    public void publish(User recipient, String title, String body, String link) {
        if (recipient == null) return; // Walk-in leads may not have an account.
        notifications.save(new AppNotification(recipient, title, body, link));
    }
    public List<NotificationDto> list(User user) { return notifications.findTop100ByRecipientOrderByCreatedAtDesc(user).stream().map(NotificationDto::from).toList(); }
    public long unread(User user) { return notifications.countByRecipientAndReadAtIsNull(user); }
    @Transactional
    public NotificationDto read(User user, Long id) {
        AppNotification n = notifications.findByIdAndRecipient(id, user).orElseThrow(() -> new NotFoundException("Notification not found"));
        if (n.getReadAt() == null) n.setReadAt(Instant.now());
        return NotificationDto.from(notifications.save(n));
    }
    @Transactional
    public void readAll(User user) { notifications.markAllRead(user, Instant.now()); }
}
