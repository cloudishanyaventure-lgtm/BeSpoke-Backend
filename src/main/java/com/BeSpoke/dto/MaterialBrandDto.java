package com.BeSpoke.dto;

import com.BeSpoke.entity.MaterialBrand;
import com.BeSpoke.entity.MaterialCategory;

import java.util.List;

public record MaterialBrandDto(
        Long id,
        String slug,
        String name,
        String note,
        String imageUrl,
        boolean active,
        List<String> categorySlugs
) {

    public static MaterialBrandDto from(MaterialBrand b) {
        return new MaterialBrandDto(b.getId(), b.getSlug(), b.getName(), b.getNote(),
                b.getImageUrl(), b.isActive(),
                b.getCategories().stream().map(MaterialCategory::getSlug).toList());
    }

    /** Card-sized form for the category pane — skips loading the category set. */
    public static MaterialBrandDto brief(MaterialBrand b) {
        return new MaterialBrandDto(b.getId(), b.getSlug(), b.getName(), b.getNote(),
                b.getImageUrl(), b.isActive(), List.of());
    }
}
