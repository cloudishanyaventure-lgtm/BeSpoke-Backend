package com.BeSpoke.dto;
import com.BeSpoke.entity.RoomPlan;
import java.time.Instant;
public record RoomPlanDto(Long id, Long version, Long leadId, String name, Integer wall, Integer floor,
        Integer fabric, Double width, Double depth, String layout, Boolean rug, Instant updatedAt) {
    public static RoomPlanDto from(RoomPlan p) {
        return new RoomPlanDto(p.getId(), p.getVersion(), p.getLead().getId(), p.getName(), p.getWall(),
                p.getFloor(), p.getFabric(), p.getWidth(), p.getDepth(), p.getLayout(), p.getRug(), p.getUpdatedAt());
    }
}
