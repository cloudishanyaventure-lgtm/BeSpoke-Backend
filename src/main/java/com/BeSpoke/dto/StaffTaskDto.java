package com.BeSpoke.dto;

import com.BeSpoke.entity.StaffTask;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.LocalDate;

/** A task as the studio board shows it. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StaffTaskDto(
        Long id,
        String title,
        String details,
        Long assigneeId,
        String assigneeName,
        String assigneeRole,
        Long createdById,
        String createdByName,
        LocalDate dueDate,
        String status,
        Instant completedAt,
        Instant createdAt,
        String visibility,
        String priority,
        boolean canUpdate
) {
    public static StaffTaskDto from(StaffTask task) {
        return from(task, false);
    }
    public static StaffTaskDto from(StaffTask task, boolean canUpdate) {
        return new StaffTaskDto(
                task.getId(),
                task.getTitle(),
                task.getDetails(),
                task.getAssignee().getId(),
                task.getAssignee().getName(),
                task.getAssignee().getRole().name(),
                task.getCreatedBy().getId(),
                task.getCreatedBy().getName(),
                task.getDueDate(),
                task.getStatus().name(),
                task.getCompletedAt(),
                task.getCreatedAt(), task.getVisibility().name(), task.getPriority().name(), canUpdate);
    }
}
