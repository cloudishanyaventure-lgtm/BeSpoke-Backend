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

    /**
     * Starter photo per category, keyed by slug. The JSON files carry a search keyword
     * rather than a picture, so these are the shipped defaults — written once into
     * image_url, after which the admin's edit in the CRM owns the field.
     */
    private static final Map<String, String> STARTER_PHOTO = Map.ofEntries(
            Map.entry("plywood-wood-boards", "1700973408133-b45276ec8feb"),
            Map.entry("laminates", "1678794792916-e5cb1217bed1"),
            Map.entry("veneers", "1591709990007-25dd40fa4b63"),
            Map.entry("acrylic-high-gloss", "1748680223727-bdaf1e7f5465"),
            Map.entry("modular-furniture-hardware", "1591640891024-3ee88673e639"),
            Map.entry("kitchen-accessories", "1678108040468-0cc9addd984d"),
            Map.entry("handles-knobs", "1607710533910-d7cdffd9e593"),
            Map.entry("glass-mirrors", "1765766600457-abfd14dd502c"),
            Map.entry("tiles-stone", "1584354273341-3eb96574e5be"),
            Map.entry("quartz-countertops", "1690310588533-6043216b0b5f"),
            Map.entry("flooring", "1560184897-1ee3713708ee"),
            Map.entry("wall-finishes", "1555181937-efe4e074a301"),
            Map.entry("paints-coatings", "1581079289196-67865ea83118"),
            Map.entry("ceiling-partition", "1551770873-897f127385d5"),
            Map.entry("electrical-smart-home", "1556217994-22de7face210"),
            Map.entry("lighting", "1540932239986-30128078f3c5"),
            Map.entry("bathroom-sanitary", "1584622650111-993a426fbf0a"),
            Map.entry("kitchen-appliances", "1543503103-f94a0036ed9d"),
            Map.entry("doors-windows", "1613324061338-19d4528a5be9"),
            Map.entry("door-hardware-locks", "1563417994968-13665a6ff908"),
            Map.entry("adhesives-sealants", "1768839725085-829e6ac7ac26"),
            Map.entry("acoustic-materials", "1675528030748-d463ab87e8d5"),
            Map.entry("soft-furnishing", "1771039621838-4c8dd6ae0f9d"),
            Map.entry("furniture", "1616486338812-3dadae4b4ace"),
            Map.entry("decorative-materials", "1774305097322-f6835d39527b"),
            Map.entry("outdoor-balcony", "1565985482558-4c32923fb2d3"));

    /** The photo URL for a category, or null when we ship no picture for that slug. */
    private static String starterPhoto(String slug) {
        String id = STARTER_PHOTO.get(slug);
        return id == null ? null
                : "https://images.unsplash.com/photo-" + id + "?q=80&w=1200&auto=format&fit=crop";
    }

    /**
     * Early rows stored the JSON search keyword ("plywood stack") in image_url, which is
     * not a picture and rendered as a broken tile. Replace those with the starter photo;
     * a real URL — an admin upload or an earlier backfill — is never touched.
     */
    private void backfillCategoryPhotos() {
        int fixed = 0;
        for (MaterialCategory category : categoryRepository.findAll()) {
            String current = category.getImageUrl();
            if (current != null && (current.startsWith("http") || current.startsWith("/"))) {
                continue;
            }
            String photo = starterPhoto(category.getSlug());
            if (photo == null) {
                continue;
            }
            category.setImageUrl(photo);
            categoryRepository.save(category);
            fixed++;
        }
        if (fixed > 0) {
            log.info("Backfilled {} category photo(s) that held a keyword instead of a URL", fixed);
        }
    }

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
        backfillCategoryPhotos();
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
            // parsed.image is a search keyword, not a picture — ship a real photo instead.
            category.setImageUrl(starterPhoto(parsed.slug));
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
