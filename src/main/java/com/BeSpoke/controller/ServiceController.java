package com.BeSpoke.controller;

import com.BeSpoke.dto.ServiceDto;
import com.BeSpoke.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final CatalogService catalogService;

    public ServiceController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public List<ServiceDto> list(@RequestParam(required = false) String category) {
        return catalogService.listServices(category);
    }

    @GetMapping("/{id}")
    public ServiceDto get(@PathVariable Long id) {
        return catalogService.getService(id);
    }
}
