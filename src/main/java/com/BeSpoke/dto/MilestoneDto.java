package com.BeSpoke.dto;

import com.BeSpoke.entity.ProjectMilestone;

import java.time.LocalDate;

public record MilestoneDto(
        Long id,
        String title,
        LocalDate plannedDate,
        LocalDate actualDate,
        boolean done,
        int sortOrder
) {

    public static MilestoneDto from(ProjectMilestone milestone) {
        return new MilestoneDto(milestone.getId(), milestone.getTitle(), milestone.getPlannedDate(),
                milestone.getActualDate(), milestone.isDone(), milestone.getSortOrder());
    }
}
