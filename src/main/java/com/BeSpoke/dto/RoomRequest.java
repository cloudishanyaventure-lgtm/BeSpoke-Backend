package com.BeSpoke.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** One room in the rooms-replacement payload. List order defines sortOrder. */
public record RoomRequest(
        @NotBlank @Size(max = 60) String roomType,
        @NotBlank @Size(max = 255) String label,
        @Size(max = 60) String floor,
        @Size(max = 120) String familyMember,
        @Size(max = 255) String primaryUse,
        @Size(max = 1000) String mustHaves,
        @Size(max = 500) String reuseFurniture,
        @Size(max = 30) String storageNeeds,
        @Size(max = 255) String colorPreference,
        @Size(max = 1000) String specialRequirements,
        @Size(max = 1000) String notes,
        /** Feet. 0 is a valid answer — "not applicable" — and is never used in a calculation. */
        @jakarta.validation.constraints.PositiveOrZero Double heightFt,
        @jakarta.validation.constraints.PositiveOrZero Double lengthFt,
        @jakarta.validation.constraints.PositiveOrZero Double widthFt,
        @jakarta.validation.constraints.PositiveOrZero Double depthFt,
        @Valid List<RoomItemRequest> items
) {

    public record RoomItemRequest(
            @NotBlank @Size(max = 255) String category,
            @NotBlank @Size(max = 500) String item,
            @Size(max = 500) String note
    ) {
    }
}
