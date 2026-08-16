package com.BeSpoke.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One specification-level entry in the library — "BWP marine plywood", "PGVT tile",
 * "COB downlight". Everything a designer filters or specifies on lives here flat:
 * the doc's eight headline filters plus the technical ones.
 */
@Entity
@Table(name = "materials")
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private MaterialCategory category;

    @Column(nullable = false, unique = true, length = 140)
    private String slug;

    @Column(nullable = false, length = 160)
    private String name;

    /** One line, shown on the card. */
    @Column(length = 400)
    private String blurb;

    @Column(length = 4000)
    private String description;

    /** Free-form "Key: Value" lines — rendered as the spec table, edited as text. */
    @Column(length = 3000)
    private String specs;

    /** Comma-separated: "Kitchen, Wardrobe, Ceiling". */
    @Column(length = 500)
    private String applications;

    @Column(length = 120)
    private String finish;

    @Column(length = 120)
    private String colour;

    @Column(length = 120)
    private String texture;

    @Column(length = 120)
    private String thickness;

    @Column(length = 120)
    private String sheetSize;

    /** IS/BIS or other standard the product is specified against. */
    @Column(length = 160)
    private String standard;

    @Column(length = 160)
    private String warranty;

    @Column(length = 200)
    private String installation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private MaterialTier tier = MaterialTier.MID;

    @Column(precision = 12, scale = 2)
    private BigDecimal priceMin;

    @Column(precision = 12, scale = 2)
    private BigDecimal priceMax;

    /** "per sq.ft", "per sheet", "per piece" … */
    @Column(length = 60)
    private String priceUnit;

    @Column(nullable = false)
    private boolean waterResistant;

    @Column(nullable = false)
    private boolean fireResistant;

    @Column(nullable = false)
    private boolean scratchResistant;

    /** Indoor | Outdoor | Indoor & outdoor. */
    @Column(length = 40)
    private String usage;

    @Column(length = 1000)
    private String imageUrl;

    /** Keyword behind the fallback photo when no imageUrl is set. */
    @Column(length = 120)
    private String imageKeyword;

    @Column(nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public MaterialCategory getCategory() {
        return category;
    }

    public void setCategory(MaterialCategory category) {
        this.category = category;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBlurb() {
        return blurb;
    }

    public void setBlurb(String blurb) {
        this.blurb = blurb;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSpecs() {
        return specs;
    }

    public void setSpecs(String specs) {
        this.specs = specs;
    }

    public String getApplications() {
        return applications;
    }

    public void setApplications(String applications) {
        this.applications = applications;
    }

    public String getFinish() {
        return finish;
    }

    public void setFinish(String finish) {
        this.finish = finish;
    }

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public String getTexture() {
        return texture;
    }

    public void setTexture(String texture) {
        this.texture = texture;
    }

    public String getThickness() {
        return thickness;
    }

    public void setThickness(String thickness) {
        this.thickness = thickness;
    }

    public String getSheetSize() {
        return sheetSize;
    }

    public void setSheetSize(String sheetSize) {
        this.sheetSize = sheetSize;
    }

    public String getStandard() {
        return standard;
    }

    public void setStandard(String standard) {
        this.standard = standard;
    }

    public String getWarranty() {
        return warranty;
    }

    public void setWarranty(String warranty) {
        this.warranty = warranty;
    }

    public String getInstallation() {
        return installation;
    }

    public void setInstallation(String installation) {
        this.installation = installation;
    }

    public MaterialTier getTier() {
        return tier;
    }

    public void setTier(MaterialTier tier) {
        this.tier = tier;
    }

    public BigDecimal getPriceMin() {
        return priceMin;
    }

    public void setPriceMin(BigDecimal priceMin) {
        this.priceMin = priceMin;
    }

    public BigDecimal getPriceMax() {
        return priceMax;
    }

    public void setPriceMax(BigDecimal priceMax) {
        this.priceMax = priceMax;
    }

    public String getPriceUnit() {
        return priceUnit;
    }

    public void setPriceUnit(String priceUnit) {
        this.priceUnit = priceUnit;
    }

    public boolean isWaterResistant() {
        return waterResistant;
    }

    public void setWaterResistant(boolean waterResistant) {
        this.waterResistant = waterResistant;
    }

    public boolean isFireResistant() {
        return fireResistant;
    }

    public void setFireResistant(boolean fireResistant) {
        this.fireResistant = fireResistant;
    }

    public boolean isScratchResistant() {
        return scratchResistant;
    }

    public void setScratchResistant(boolean scratchResistant) {
        this.scratchResistant = scratchResistant;
    }

    public String getUsage() {
        return usage;
    }

    public void setUsage(String usage) {
        this.usage = usage;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImageKeyword() {
        return imageKeyword;
    }

    public void setImageKeyword(String imageKeyword) {
        this.imageKeyword = imageKeyword;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
