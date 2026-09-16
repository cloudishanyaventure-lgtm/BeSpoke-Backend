package com.BeSpoke.dto;

import com.BeSpoke.entity.RequirementRoom;

import java.util.List;

public record RoomDto(
        Long id,
        String roomType,
        String label,
        String floor,
        String familyMember,
        String primaryUse,
        String mustHaves,
        String reuseFurniture,
        String storageNeeds,
        String colorPreference,
        String specialRequirements,
        String notes,
        Double heightFt,
        Double lengthFt,
        Double widthFt,
        Double depthFt,
        /** Length x width in square feet, or null when either is 0 / not given. */
        Double areaSqft,
        int sortOrder,
        List<RoomItemDto> items
) {

    public static RoomDto from(RequirementRoom room) {
        return new RoomDto(
                room.getId(),
                room.getRoomType(),
                room.getLabel(),
                room.getFloor(),
                room.getFamilyMember(),
                room.getPrimaryUse(),
                room.getMustHaves(),
                room.getReuseFurniture(),
                room.getStorageNeeds(),
                room.getColorPreference(),
                room.getSpecialRequirements(),
                room.getNotes(),
                room.getHeightFt(),
                room.getLengthFt(),
                room.getWidthFt(),
                room.getDepthFt(),
                room.areaSqft(),
                room.getSortOrder(),
                room.getItems().stream().map(RoomItemDto::from).toList()
        );
    }
}
