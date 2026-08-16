package com.BeSpoke.dto;

import com.BeSpoke.entity.Material;
import com.BeSpoke.entity.MaterialTier;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public record MaterialDto(
        Long id,
        String slug,
        String name,
        String categorySlug,
        String categoryName,
        String blurb,
        String description,
        Map<String, String> specs,
        List<String> applications,
        String finish,
        String colour,
        String texture,
        String thickness,
        String sheetSize,
        String standard,
        String warranty,
        String installation,
        MaterialTier tier,
        BigDecimal priceMin,
        BigDecimal priceMax,
        String priceUnit,
        boolean waterResistant,
        boolean fireResistant,
        boolean scratchResistant,
        String usage,
        String imageUrl,
        String imageKeyword,
        int sortOrder,
        boolean active
) {

    /** "Core: Hardwood" lines in, an ordered map out — the spec table on the detail pane. */
    public static Map<String, String> parseSpecs(String raw) {
        Map<String, String> specs = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return specs;
        }
        for (String line : raw.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int colon = trimmed.indexOf(':');
            if (colon > 0) {
                specs.put(trimmed.substring(0, colon).trim(), trimmed.substring(colon + 1).trim());
            } else {
                specs.put(trimmed, "");
            }
        }
        return specs;
    }

    public static List<String> parseList(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public static MaterialDto from(Material m) {
        return new MaterialDto(m.getId(), m.getSlug(), m.getName(),
                m.getCategory().getSlug(), m.getCategory().getName(),
                m.getBlurb(), m.getDescription(),
                parseSpecs(m.getSpecs()), parseList(m.getApplications()),
                m.getFinish(), m.getColour(), m.getTexture(), m.getThickness(),
                m.getSheetSize(), m.getStandard(), m.getWarranty(), m.getInstallation(),
                m.getTier(), m.getPriceMin(), m.getPriceMax(), m.getPriceUnit(),
                m.isWaterResistant(), m.isFireResistant(), m.isScratchResistant(),
                m.getUsage(), m.getImageUrl(), m.getImageKeyword(),
                m.getSortOrder(), m.isActive());
    }
}
