package com.BeSpoke.controller;

import com.BeSpoke.dto.DesignerDto;
import com.BeSpoke.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/designers")
public class DesignerController {

    private final CatalogService catalogService;

    public DesignerController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public List<DesignerDto> list() {
        return catalogService.listDesigners();
    }

    @GetMapping("/{id}")
    public DesignerDto get(@PathVariable Long id) {
        return catalogService.getDesigner(id);
    }
}
