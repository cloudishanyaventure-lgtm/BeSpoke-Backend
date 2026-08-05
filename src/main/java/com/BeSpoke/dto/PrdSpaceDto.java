package com.BeSpoke.dto;

import com.BeSpoke.entity.RequirementRoom;

/** A PRD room reduced to what the drawing-upload picker needs. */
public record PrdSpaceDto(Long id, String label, String floor, String familyMember) {

    public static PrdSpaceDto from(RequirementRoom room) {
        return new PrdSpaceDto(room.getId(), room.getLabel(), room.getFloor(), room.getFamilyMember());
    }
}
