package com.BeSpoke.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name = "app_notifications", indexes = @Index(name = "notification_user_created", columnList = "recipient_id,created_at"))
public class AppNotification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "recipient_id", nullable = false) private User recipient;
    @Column(nullable = false, length = 200) private String title;
    @Column(nullable = false, length = 1000) private String body;
    @Column(nullable = false, length = 200) private String link;
    @Column(nullable = false) private Instant createdAt = Instant.now();
    private Instant readAt;
    public AppNotification() {}
    public AppNotification(User recipient, String title, String body, String link) { this.recipient = recipient; this.title = title; this.body = body; this.link = link; }
    public Long getId() { return id; }
    public User getRecipient() { return recipient; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getLink() { return link; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant at) { readAt = at; }
}
