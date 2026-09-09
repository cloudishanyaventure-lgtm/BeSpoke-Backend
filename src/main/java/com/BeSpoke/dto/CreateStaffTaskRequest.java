package com.BeSpoke.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Hand a piece of work to a colleague. The assignee is tagged from your own team. */
public record CreateStaffTaskRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String details,
        @NotNull Long assigneeId,
        LocalDate dueDate,
        @jakarta.validation.constraints.Pattern(regexp = "PUBLIC|PRIVATE") String visibility,
        @jakarta.validation.constraints.Pattern(regexp = "LOW|NORMAL|HIGH|URGENT") String priority
) {
    public CreateStaffTaskRequest(String title, String details, Long assigneeId, LocalDate dueDate) {
        this(title, details, assigneeId, dueDate, "PRIVATE", "NORMAL");
    }
}
