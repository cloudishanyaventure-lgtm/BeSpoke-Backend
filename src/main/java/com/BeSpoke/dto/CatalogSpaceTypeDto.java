package com.BeSpoke.dto;

import java.util.List;

/** The room-item catalog grouped for the wizard: spaceType -> categories -> items. */
public record CatalogSpaceTypeDto(String spaceType, List<CatalogCategoryDto> categories) {

    public record CatalogCategoryDto(String category, List<String> items) {
    }
}
