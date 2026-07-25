package com.BeSpoke.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** id present = update that milestone; absent = create a new one. Missing ids are deleted. */
public record MilestoneRequest(
        Long id,
        @NotBlank @Size(max = 255) String title,
        LocalDate plannedDate,
        LocalDate actualDate,
        Boolean done,
        Integer sortOrder
) {
}
