package com.BeSpoke.dto;

import com.BeSpoke.entity.DesignService;

import java.math.BigDecimal;

public record ServiceDto(
        Long id,
        String title,
        String category,
        String description,
        BigDecimal price,
        String imageUrl,
        String deliverables
) {

    public static ServiceDto from(DesignService s) {
        return new ServiceDto(
                s.getId(),
                s.getTitle(),
                s.getCategory().name(),
                s.getDescription(),
                s.getPrice(),
                s.getImageUrl(),
                s.getDeliverables()
        );
    }
}
