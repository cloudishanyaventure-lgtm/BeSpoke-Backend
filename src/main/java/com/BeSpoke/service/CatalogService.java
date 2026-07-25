package com.BeSpoke.service;

import com.BeSpoke.dto.CatalogSpaceTypeDto;
import com.BeSpoke.entity.RoomCatalogItem;
import com.BeSpoke.repository.RoomCatalogItemRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CatalogService {

    private final RoomCatalogItemRepository roomCatalogItemRepository;

    public CatalogService(RoomCatalogItemRepository roomCatalogItemRepository) {
        this.roomCatalogItemRepository = roomCatalogItemRepository;
    }

    /** Groups the seeded catalog rows as [{spaceType, categories:[{category, items:[...]}]}]. */
    public List<CatalogSpaceTypeDto> roomItems() {
        Map<String, Map<String, List<String>>> grouped = new LinkedHashMap<>();
        for (RoomCatalogItem item : roomCatalogItemRepository.findAllByOrderBySortOrderAsc()) {
            grouped.computeIfAbsent(item.getSpaceType(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(item.getCategory(), k -> new ArrayList<>())
                    .add(item.getItem());
        }
        List<CatalogSpaceTypeDto> result = new ArrayList<>();
        grouped.forEach((spaceType, categories) -> {
            List<CatalogSpaceTypeDto.CatalogCategoryDto> categoryDtos = new ArrayList<>();
            categories.forEach((category, items) ->
                    categoryDtos.add(new CatalogSpaceTypeDto.CatalogCategoryDto(category, items)));
            result.add(new CatalogSpaceTypeDto(spaceType, categoryDtos));
        });
        return result;
    }
}
