package com.BeSpoke.dto;

import com.BeSpoke.entity.RequirementRoomItem;

public record RoomItemDto(String category, String item, String note) {

    public static RoomItemDto from(RequirementRoomItem item) {
        return new RoomItemDto(item.getCategory(), item.getItem(), item.getNote());
    }
}
