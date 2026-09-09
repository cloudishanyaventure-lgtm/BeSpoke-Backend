package com.BeSpoke.dto;
import java.time.Instant;
import java.util.Set;
public record TaskCommentDto(Long id, Long authorId, String authorName, String body,
                             Set<Long> mentionIds, boolean systemEvent, Instant createdAt) {}
