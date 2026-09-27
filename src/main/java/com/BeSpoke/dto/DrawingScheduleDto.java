package com.BeSpoke.dto;

import com.BeSpoke.entity.DrawingSchedule;
import com.BeSpoke.entity.DrawingScheduleItem;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * The timeline as the studio page needs it. A persisted schedule has an {@code id}; a
 * freshly proposed one (built from the BOQ, not yet saved) has {@code id == null} and
 * {@code status == DRAFT} with blank dates. {@code editable} is false once approved.
 */
public record DrawingScheduleDto(
        Long id,
        Long leadId,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        boolean editable,
        String submittedByName,
        Instant submittedAt,
        String approvedByName,
        Instant approvedAt,
        List<Item> items,
        /** True when the BOQ has been approved and a timeline can be built/kept. */
        boolean boqApproved
) {

    public record Item(
            Long id,
            String section,
            String label,
            int orderIndex,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            Instant startedAt,
            Instant completedAt
    ) {
        public static Item from(DrawingScheduleItem it) {
            return new Item(it.getId(), it.getSection(), it.getLabel(), it.getOrderIndex(),
                    it.getStartDate(), it.getEndDate(), it.getStatus().name(),
                    it.getStartedAt(), it.getCompletedAt());
        }
    }

    public static DrawingScheduleDto from(DrawingSchedule s, boolean boqApproved) {
        boolean editable = s.getStatus() != DrawingSchedule.Status.APPROVED;
        return new DrawingScheduleDto(s.getId(), s.getLead().getId(), s.getStartDate(),
                s.getEndDate(), s.getStatus().name(), editable, s.getSubmittedByName(),
                s.getSubmittedAt(), s.getApprovedByName(), s.getApprovedAt(),
                s.getItems().stream().map(Item::from).toList(), boqApproved);
    }
}
