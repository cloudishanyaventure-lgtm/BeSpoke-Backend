package com.BeSpoke.controller;
import com.BeSpoke.dto.NotificationDto;
import com.BeSpoke.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notifications;
    private final CurrentUserService users;
    public NotificationController(NotificationService notifications, CurrentUserService users) { this.notifications = notifications; this.users = users; }
    @GetMapping public List<NotificationDto> list(Authentication auth) { return notifications.list(users.requireByEmail(auth.getName())); }
    @GetMapping("/unread-count") public Map<String, Long> unread(Authentication auth) { return Map.of("unread", notifications.unread(users.requireByEmail(auth.getName()))); }
    @PostMapping("/{id}/read") public NotificationDto read(Authentication auth, @PathVariable Long id) { return notifications.read(users.requireByEmail(auth.getName()), id); }
    @PostMapping("/read-all") public void readAll(Authentication auth) { notifications.readAll(users.requireByEmail(auth.getName())); }
}
