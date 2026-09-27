package com.BeSpoke.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** Save the whole timeline in one shot: the outer window plus every element's slice. */
public record DrawingScheduleRequest(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @Valid List<ItemRequest> items
) {

    public record ItemRequest(
            @Size(max = 200) String section,
            @Size(max = 500) String label,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate
    ) {
    }
}
