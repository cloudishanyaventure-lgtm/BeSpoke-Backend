package com.BeSpoke.controller;

import com.BeSpoke.dto.DesignerDto;
import com.BeSpoke.dto.ReviewDto;
import com.BeSpoke.service.CatalogService;
import com.BeSpoke.service.ReviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/designers")
public class DesignerController {

    private final CatalogService catalogService;
    private final ReviewService reviewService;

    public DesignerController(CatalogService catalogService, ReviewService reviewService) {
        this.catalogService = catalogService;
        this.reviewService = reviewService;
    }

    @GetMapping
    public List<DesignerDto> list() {
        return catalogService.listDesigners();
    }

    @GetMapping("/{id}")
    public DesignerDto get(@PathVariable Long id) {
        return catalogService.getDesigner(id);
    }

    /** PUBLIC. Records a profile view for the designer with the given user id. */
    @PostMapping("/{userId}/view")
    public Map<String, Long> recordView(@PathVariable Long userId) {
        return Map.of("views", catalogService.incrementDesignerViews(userId));
    }

    /** PUBLIC. Reviews for the designer with the given user id, newest first. */
    @GetMapping("/{userId}/reviews")
    public List<ReviewDto> reviews(@PathVariable Long userId) {
        return reviewService.reviewsForDesigner(userId);
    }
}
