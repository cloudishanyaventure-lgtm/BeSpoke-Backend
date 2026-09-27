package com.BeSpoke.dto;

import com.BeSpoke.entity.RequirementRoomItem;

public record RoomItemDto(String category, String item, String note,
                          Double lengthFt, Double widthFt, Double depthFt, Double heightFt,
                          /** Length x width in square feet, or null when either is 0 / not given. */
                          Double areaSqft) {

    public static RoomItemDto from(RequirementRoomItem item) {
        return new RoomItemDto(item.getCategory(), item.getItem(), item.getNote(),
                item.getLengthFt(), item.getWidthFt(), item.getDepthFt(), item.getHeightFt(),
                item.areaSqft());
    }
}
