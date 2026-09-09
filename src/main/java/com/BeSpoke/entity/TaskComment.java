package com.BeSpoke.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "task_comments", indexes = @Index(name = "idx_task_comments_thread", columnList = "task_id,id"))
public class TaskComment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private StaffTask task;
    @ManyToOne(optional = false) private User author;
    @Column(nullable = false, length = 22000) private String body;
    @Column(nullable = false) private Instant createdAt = Instant.now();
    @ElementCollection private Set<Long> mentionIds = new LinkedHashSet<>();
    private boolean systemEvent;
    protected TaskComment() {}
    public TaskComment(StaffTask task, User author, String body, Set<Long> mentions, boolean event) {
        this.task = task; this.author = author; this.body = body;
        this.mentionIds = mentions; this.systemEvent = event;
    }
    public Long getId() { return id; }
    public User getAuthor() { return author; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }
    public Set<Long> getMentionIds() { return mentionIds; }
    public boolean isSystemEvent() { return systemEvent; }
}
