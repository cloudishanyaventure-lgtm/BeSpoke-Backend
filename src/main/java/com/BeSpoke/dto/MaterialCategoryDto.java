package com.BeSpoke.dto;

import com.BeSpoke.entity.MaterialCategory;

import java.util.List;

public record MaterialCategoryDto(
        Long id,
        String slug,
        String name,
        String tagline,
        String description,
        String imageUrl,
        int sortOrder,
        boolean active,
        long materialCount,
        List<MaterialBrandDto> brands
) {

    public static MaterialCategoryDto from(MaterialCategory c, long count,
                                           List<MaterialBrandDto> brands) {
        return new MaterialCategoryDto(c.getId(), c.getSlug(), c.getName(), c.getTagline(),
                c.getDescription(), c.getImageUrl(), c.getSortOrder(), c.isActive(),
                count, brands);
    }
}
