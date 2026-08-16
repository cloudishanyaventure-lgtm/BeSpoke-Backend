package com.BeSpoke.controller;

import com.BeSpoke.dto.MaterialBrandDto;
import com.BeSpoke.dto.MaterialBrandRequest;
import com.BeSpoke.dto.MaterialCategoryDto;
import com.BeSpoke.dto.MaterialCategoryRequest;
import com.BeSpoke.dto.MaterialDto;
import com.BeSpoke.dto.MaterialRequest;
import com.BeSpoke.entity.MaterialTier;
import com.BeSpoke.service.MaterialLibraryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * The material library. Reads are public (the website browses it); every write is
 * platform-side — only BeSpoke's own admins curate the library.
 */
@RestController
@RequestMapping("/api/materials")
public class MaterialController {

    private static final String PLATFORM = "hasAnyRole('SUPER_ADMIN','ADMIN')";

    private final MaterialLibraryService service;

    public MaterialController(MaterialLibraryService service) {
        this.service = service;
    }

    // ---- public browse ----

    @GetMapping("/categories")
    public List<MaterialCategoryDto> categories(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return service.categories(includeInactive);
    }

    @GetMapping("/categories/{slug}")
    public MaterialCategoryDto category(@PathVariable String slug) {
        return service.category(slug);
    }

    @GetMapping("/brands")
    public List<MaterialBrandDto> brands() {
        return service.brands();
    }

    @GetMapping
    public List<MaterialDto> browse(@RequestParam(required = false) String category,
                                    @RequestParam(required = false) MaterialTier tier,
                                    @RequestParam(required = false) String usage,
                                    @RequestParam(required = false) String finish,
                                    @RequestParam(required = false) Boolean water,
                                    @RequestParam(required = false) Boolean fire,
                                    @RequestParam(required = false) Boolean scratch,
                                    @RequestParam(required = false) BigDecimal maxPrice,
                                    @RequestParam(required = false) String q) {
        return service.browse(category, tier, usage, finish, water, fire, scratch, maxPrice, q);
    }

    @GetMapping("/{slug}")
    public MaterialDto material(@PathVariable String slug) {
        return service.material(slug);
    }

    @GetMapping("/{slug}/similar")
    public List<MaterialDto> similar(@PathVariable String slug) {
        return service.similar(slug);
    }

    // ---- platform admin ----

    @PostMapping("/categories")
    @PreAuthorize(PLATFORM)
    public ResponseEntity<MaterialCategoryDto> createCategory(
            @Valid @RequestBody MaterialCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize(PLATFORM)
    public MaterialCategoryDto updateCategory(@PathVariable Long id,
                                              @Valid @RequestBody MaterialCategoryRequest request) {
        return service.updateCategory(id, request);
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize(PLATFORM)
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        service.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/brands")
    @PreAuthorize(PLATFORM)
    public ResponseEntity<MaterialBrandDto> createBrand(
            @Valid @RequestBody MaterialBrandRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createBrand(request));
    }

    @PutMapping("/brands/{id}")
    @PreAuthorize(PLATFORM)
    public MaterialBrandDto updateBrand(@PathVariable Long id,
                                        @Valid @RequestBody MaterialBrandRequest request) {
        return service.updateBrand(id, request);
    }

    @DeleteMapping("/brands/{id}")
    @PreAuthorize(PLATFORM)
    public ResponseEntity<Void> deleteBrand(@PathVariable Long id) {
        service.deleteBrand(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @PreAuthorize(PLATFORM)
    public ResponseEntity<MaterialDto> create(@Valid @RequestBody MaterialRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createMaterial(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(PLATFORM)
    public MaterialDto update(@PathVariable Long id,
                              @Valid @RequestBody MaterialRequest request) {
        return service.updateMaterial(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(PLATFORM)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteMaterial(id);
        return ResponseEntity.noContent().build();
    }
}
