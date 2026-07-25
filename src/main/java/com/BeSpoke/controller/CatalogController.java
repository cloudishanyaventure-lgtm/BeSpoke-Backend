package com.BeSpoke.controller;

import com.BeSpoke.dto.CatalogSpaceTypeDto;
import com.BeSpoke.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public catalog for the requirement wizard. */
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/room-items")
    public List<CatalogSpaceTypeDto> roomItems() {
        return catalogService.roomItems();
    }
}
