package com.BeSpoke.service;

import com.BeSpoke.dto.MaterialBrandDto;
import com.BeSpoke.dto.MaterialBrandRequest;
import com.BeSpoke.dto.MaterialCategoryDto;
import com.BeSpoke.dto.MaterialCategoryRequest;
import com.BeSpoke.dto.MaterialDto;
import com.BeSpoke.dto.MaterialRequest;
import com.BeSpoke.entity.Material;
import com.BeSpoke.entity.MaterialBrand;
import com.BeSpoke.entity.MaterialCategory;
import com.BeSpoke.entity.MaterialTier;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.MaterialBrandRepository;
import com.BeSpoke.repository.MaterialCategoryRepository;
import com.BeSpoke.repository.MaterialRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.LinkedHashSet;

/** Browse + platform-admin upkeep for the material library. */
@Service
public class MaterialLibraryService {

    private static final int SIMILAR_LIMIT = 6;

    private final MaterialCategoryRepository categoryRepository;
    private final MaterialBrandRepository brandRepository;
    private final MaterialRepository materialRepository;

    public MaterialLibraryService(MaterialCategoryRepository categoryRepository,
                                  MaterialBrandRepository brandRepository,
                                  MaterialRepository materialRepository) {
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.materialRepository = materialRepository;
    }

    // ---- browse ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<MaterialCategoryDto> categories(boolean includeInactive) {
        List<MaterialCategory> categories = includeInactive
                ? categoryRepository.findAllByOrderBySortOrderAscNameAsc()
                : categoryRepository.findByActiveTrueOrderBySortOrderAscNameAsc();
        return categories.stream()
                .map(c -> MaterialCategoryDto.from(c,
                        materialRepository.countByCategoryIdAndActiveTrue(c.getId()),
                        brandRepository.findByCategorySlug(c.getSlug()).stream()
                                .map(MaterialBrandDto::brief).toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public MaterialCategoryDto category(String slug) {
        MaterialCategory category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("No such category"));
        return MaterialCategoryDto.from(category,
                materialRepository.countByCategoryIdAndActiveTrue(category.getId()),
                brandRepository.findByCategorySlug(slug).stream()
                        .map(MaterialBrandDto::brief).toList());
    }

    @Transactional(readOnly = true)
    public List<MaterialDto> browse(String category, MaterialTier tier, String usage,
                                    String finish, Boolean water, Boolean fire,
                                    Boolean scratch, BigDecimal maxPrice, String q) {
        Specification<Material> spec = (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            where.add(cb.isTrue(root.get("active")));
            if (blankToNull(category) != null) {
                where.add(cb.equal(root.get("category").get("slug"), category.trim()));
            }
            if (tier != null) {
                where.add(cb.equal(root.get("tier"), tier));
            }
            if (blankToNull(usage) != null) {
                where.add(like(cb, root.get("usage"), usage));
            }
            if (blankToNull(finish) != null) {
                where.add(like(cb, root.get("finish"), finish));
            }
            if (Boolean.TRUE.equals(water)) {
                where.add(cb.isTrue(root.get("waterResistant")));
            }
            if (Boolean.TRUE.equals(fire)) {
                where.add(cb.isTrue(root.get("fireResistant")));
            }
            if (Boolean.TRUE.equals(scratch)) {
                where.add(cb.isTrue(root.get("scratchResistant")));
            }
            if (maxPrice != null) {
                // No price recorded still shows — an unpriced item isn't "too expensive".
                where.add(cb.or(cb.isNull(root.get("priceMin")),
                        cb.lessThanOrEqualTo(root.get("priceMin"), maxPrice)));
            }
            if (blankToNull(q) != null) {
                where.add(cb.or(like(cb, root.get("name"), q), like(cb, root.get("blurb"), q),
                        like(cb, root.get("description"), q),
                        like(cb, root.get("applications"), q)));
            }
            return cb.and(where.toArray(new Predicate[0]));
        };
        return materialRepository
                .findAll(spec, Sort.by("category.sortOrder", "sortOrder", "name"))
                .stream().map(MaterialDto::from).toList();
    }

    private static Predicate like(CriteriaBuilder cb, Path<String> field, String value) {
        return cb.like(cb.lower(field), "%" + value.trim().toLowerCase(Locale.ROOT) + "%");
    }

    @Transactional(readOnly = true)
    public MaterialDto material(String slug) {
        return MaterialDto.from(materialRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("No such material")));
    }

    @Transactional(readOnly = true)
    public List<MaterialDto> similar(String slug) {
        Material material = materialRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("No such material"));
        return materialRepository
                .findSimilar(material.getCategory().getId(), material.getId())
                .stream().limit(SIMILAR_LIMIT).map(MaterialDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<MaterialBrandDto> brands() {
        return brandRepository.findAllByOrderByNameAsc().stream()
                .map(MaterialBrandDto::from).toList();
    }

    // ---- admin upkeep ----------------------------------------------------------

    @Transactional
    public MaterialCategoryDto createCategory(MaterialCategoryRequest request) {
        MaterialCategory category = new MaterialCategory();
        category.setSlug(uniqueCategorySlug(request.slug(), request.name()));
        category.setSortOrder(request.sortOrder() != null ? request.sortOrder()
                : categoryRepository.count() > 0 ? (int) categoryRepository.count() + 1 : 1);
        apply(category, request);
        return MaterialCategoryDto.from(categoryRepository.save(category), 0, List.of());
    }

    @Transactional
    public MaterialCategoryDto updateCategory(Long id, MaterialCategoryRequest request) {
        MaterialCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No such category"));
        if (request.slug() != null && !request.slug().isBlank()
                && !request.slug().equals(category.getSlug())) {
            category.setSlug(uniqueCategorySlug(request.slug(), request.name()));
        }
        if (request.sortOrder() != null) {
            category.setSortOrder(request.sortOrder());
        }
        apply(category, request);
        return MaterialCategoryDto.from(categoryRepository.save(category),
                materialRepository.countByCategoryIdAndActiveTrue(category.getId()),
                List.of());
    }

    private void apply(MaterialCategory category, MaterialCategoryRequest request) {
        category.setName(request.name().trim());
        category.setTagline(request.tagline());
        category.setDescription(request.description());
        category.setImageUrl(request.imageUrl());
        if (request.active() != null) {
            category.setActive(request.active());
        }
    }

    /** Refuses while materials still hang off it — deactivate instead, or move them. */
    @Transactional
    public void deleteCategory(Long id) {
        MaterialCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No such category"));
        if (!materialRepository.findByCategoryIdOrderBySortOrderAscNameAsc(id).isEmpty()) {
            throw new BadRequestException(
                    "This category still has materials — delete or move them first");
        }
        categoryRepository.delete(category);
    }

    @Transactional
    public MaterialDto createMaterial(MaterialRequest request) {
        Material material = new Material();
        material.setCategory(categoryBySlug(request.categorySlug()));
        material.setSlug(uniqueMaterialSlug(request.slug(), request.name()));
        material.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 999);
        apply(material, request);
        return MaterialDto.from(materialRepository.save(material));
    }

    @Transactional
    public MaterialDto updateMaterial(Long id, MaterialRequest request) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No such material"));
        material.setCategory(categoryBySlug(request.categorySlug()));
        if (request.slug() != null && !request.slug().isBlank()
                && !request.slug().equals(material.getSlug())) {
            material.setSlug(uniqueMaterialSlug(request.slug(), request.name()));
        }
        if (request.sortOrder() != null) {
            material.setSortOrder(request.sortOrder());
        }
        apply(material, request);
        return MaterialDto.from(materialRepository.save(material));
    }

    private void apply(Material material, MaterialRequest request) {
        material.setName(request.name().trim());
        material.setBlurb(request.blurb());
        material.setDescription(request.description());
        material.setSpecs(request.specs());
        material.setApplications(request.applications());
        material.setFinish(request.finish());
        material.setColour(request.colour());
        material.setTexture(request.texture());
        material.setThickness(request.thickness());
        material.setSheetSize(request.sheetSize());
        material.setStandard(request.standard());
        material.setWarranty(request.warranty());
        material.setInstallation(request.installation());
        material.setTier(request.tier() != null ? request.tier() : MaterialTier.MID);
        material.setPriceMin(request.priceMin());
        material.setPriceMax(request.priceMax());
        material.setPriceUnit(request.priceUnit());
        material.setWaterResistant(Boolean.TRUE.equals(request.waterResistant()));
        material.setFireResistant(Boolean.TRUE.equals(request.fireResistant()));
        material.setScratchResistant(Boolean.TRUE.equals(request.scratchResistant()));
        material.setUsage(request.usage());
        material.setImageUrl(request.imageUrl());
        material.setImageKeyword(request.imageKeyword());
        if (request.active() != null) {
            material.setActive(request.active());
        }
    }

    @Transactional
    public void deleteMaterial(Long id) {
        materialRepository.delete(materialRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No such material")));
    }

    @Transactional
    public MaterialBrandDto createBrand(MaterialBrandRequest request) {
        MaterialBrand brand = new MaterialBrand();
        brand.setSlug(uniqueBrandSlug(request.slug(), request.name()));
        apply(brand, request);
        return MaterialBrandDto.from(brandRepository.save(brand));
    }

    @Transactional
    public MaterialBrandDto updateBrand(Long id, MaterialBrandRequest request) {
        MaterialBrand brand = brandRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No such brand"));
        if (request.slug() != null && !request.slug().isBlank()
                && !request.slug().equals(brand.getSlug())) {
            brand.setSlug(uniqueBrandSlug(request.slug(), request.name()));
        }
        apply(brand, request);
        return MaterialBrandDto.from(brandRepository.save(brand));
    }

    private void apply(MaterialBrand brand, MaterialBrandRequest request) {
        brand.setName(request.name().trim());
        brand.setNote(request.note());
        brand.setImageUrl(request.imageUrl());
        if (request.categorySlugs() != null) {
            Set<MaterialCategory> categories = new LinkedHashSet<>();
            for (String slug : request.categorySlugs()) {
                categories.add(categoryBySlug(slug));
            }
            brand.setCategories(categories);
        }
        if (request.active() != null) {
            brand.setActive(request.active());
        }
    }

    @Transactional
    public void deleteBrand(Long id) {
        brandRepository.delete(brandRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No such brand")));
    }

    // ---- helpers ---------------------------------------------------------------

    private MaterialCategory categoryBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new BadRequestException("No such category: " + slug));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static String slugify(String value) {
        String slug = value.toLowerCase(Locale.ROOT)
                .replace("&", " and ")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return slug.isBlank() ? "item" : slug;
    }

    /** Appends -2, -3 … until the slug is free, so admins never hit a raw unique-key 500. */
    private String uniqueMaterialSlug(String preferred, String name) {
        String base = slugify(preferred != null && !preferred.isBlank() ? preferred : name);
        String slug = base;
        for (int n = 2; materialRepository.findBySlug(slug).isPresent(); n++) {
            slug = base + "-" + n;
        }
        return slug;
    }

    private String uniqueCategorySlug(String preferred, String name) {
        String base = slugify(preferred != null && !preferred.isBlank() ? preferred : name);
        String slug = base;
        for (int n = 2; categoryRepository.findBySlug(slug).isPresent(); n++) {
            slug = base + "-" + n;
        }
        return slug;
    }

    private String uniqueBrandSlug(String preferred, String name) {
        String base = slugify(preferred != null && !preferred.isBlank() ? preferred : name);
        String slug = base;
        for (int n = 2; brandRepository.findBySlug(slug).isPresent(); n++) {
            slug = base + "-" + n;
        }
        return slug;
    }
}
