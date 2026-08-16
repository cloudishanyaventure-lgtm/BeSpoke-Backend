package com.BeSpoke.config;

import com.BeSpoke.entity.Material;
import com.BeSpoke.entity.MaterialBrand;
import com.BeSpoke.entity.MaterialCategory;
import com.BeSpoke.entity.MaterialTier;
import com.BeSpoke.repository.MaterialBrandRepository;
import com.BeSpoke.repository.MaterialCategoryRepository;
import com.BeSpoke.repository.MaterialRepository;
import com.BeSpoke.service.MaterialLibraryService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the material library from {@code resources/materials/*.json} — the taxonomy in
 * the BRD, one file per category.
 * <p>
 * Per category it is add-only and idempotent: a category already in the database is left
 * exactly as the admins last edited it, and only genuinely new categories are inserted.
 * So editing a seeded material in the CRM survives every restart, while dropping a new
 * category file in ships new content.
 */
@Component
public class MaterialLibrarySeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MaterialLibrarySeeder.class);
    private static final String PATTERN = "classpath:materials/*.json";

    private final MaterialCategoryRepository categoryRepository;
    private final MaterialBrandRepository brandRepository;
    private final MaterialRepository materialRepository;
    private final ObjectMapper objectMapper;

    public MaterialLibrarySeeder(MaterialCategoryRepository categoryRepository,
                                 MaterialBrandRepository brandRepository,
                                 MaterialRepository materialRepository,
                                 ObjectMapper objectMapper) {
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.materialRepository = materialRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        Resource[] files = new PathMatchingResourcePatternResolver().getResources(PATTERN);
        Arrays.sort(files, (a, b) -> String.valueOf(a.getFilename())
                .compareTo(String.valueOf(b.getFilename())));
        int categories = 0;
        int materials = 0;
        int order = 0;
        for (Resource file : files) {
            CategoryFile parsed;
            try (InputStream in = file.getInputStream()) {
                parsed = objectMapper.readValue(in, CategoryFile.class);
            }
            order++;
            if (categoryRepository.findBySlug(parsed.slug).isPresent()) {
                continue;   // already curated in the CRM — never overwrite
            }
            MaterialCategory category = new MaterialCategory();
            category.setSlug(parsed.slug);
            category.setName(parsed.name);
            category.setTagline(parsed.tagline);
            category.setDescription(parsed.description);
            category.setImageUrl(parsed.image);
            category.setSortOrder(order);
            category = categoryRepository.save(category);
            categories++;

            for (String brandName : orEmpty(parsed.brands)) {
                String slug = MaterialLibraryService.slugify(brandName);
                MaterialBrand brand = brandRepository.findBySlug(slug)
                        .orElseGet(() -> {
                            MaterialBrand fresh = new MaterialBrand();
                            fresh.setSlug(slug);
                            fresh.setName(brandName);
                            return fresh;
                        });
                brand.getCategories().add(category);
                brandRepository.save(brand);
            }

            int itemOrder = 0;
            for (MaterialFile item : orEmpty(parsed.materials)) {
                Material material = new Material();
                material.setCategory(category);
                material.setSlug(freeSlug(parsed.slug + "-" + item.name));
                material.setName(item.name);
                material.setBlurb(item.blurb);
                material.setDescription(item.description);
                material.setSpecs(specLines(item.specs));
                material.setApplications(join(item.applications));
                material.setFinish(item.finish);
                material.setColour(item.colour);
                material.setTexture(item.texture);
                material.setThickness(item.thickness);
                material.setSheetSize(item.size);
                material.setStandard(item.standard);
                material.setWarranty(item.warranty);
                material.setInstallation(item.installation);
                material.setTier(item.tier != null
                        ? MaterialTier.valueOf(item.tier.toUpperCase()) : MaterialTier.MID);
                material.setPriceMin(decimal(item.priceMin));
                material.setPriceMax(decimal(item.priceMax));
                material.setPriceUnit(item.priceUnit);
                material.setWaterResistant(Boolean.TRUE.equals(item.water));
                material.setFireResistant(Boolean.TRUE.equals(item.fire));
                material.setScratchResistant(Boolean.TRUE.equals(item.scratch));
                material.setUsage(item.use != null ? item.use : "Indoor");
                material.setImageKeyword(item.image != null ? item.image : item.name);
                material.setSortOrder(itemOrder++);
                materialRepository.save(material);
                materials++;
            }
        }
        if (categories > 0) {
            log.info("Seeded material library: {} categories, {} materials", categories, materials);
        }
    }

    /** Seeded slugs collide only if an admin already made one by hand — step past it. */
    private String freeSlug(String raw) {
        String base = MaterialLibraryService.slugify(raw);
        String slug = base;
        for (int n = 2; materialRepository.findBySlug(slug).isPresent(); n++) {
            slug = base + "-" + n;
        }
        return slug;
    }

    private static <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static String join(List<String> values) {
        return values == null || values.isEmpty() ? null : String.join(", ", values);
    }

    private static String specLines(Map<String, String> specs) {
        if (specs == null || specs.isEmpty()) {
            return null;
        }
        List<String> lines = new ArrayList<>();
        specs.forEach((key, value) -> lines.add(key + ": " + value));
        return String.join("\n", lines);
    }

    private static BigDecimal decimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class CategoryFile {
        public String slug;
        public String name;
        public String tagline;
        public String description;
        public String image;
        public List<String> brands;
        public List<MaterialFile> materials;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class MaterialFile {
        public String name;
        public String blurb;
        public String description;
        public LinkedHashMap<String, String> specs;
        public List<String> applications;
        public String finish;
        public String colour;
        public String texture;
        public String thickness;
        public String size;
        public String standard;
        public String warranty;
        public String installation;
        public String tier;
        public Double priceMin;
        public Double priceMax;
        public String priceUnit;
        public Boolean water;
        public Boolean fire;
        public Boolean scratch;
        public String use;
        public String image;
    }
}
