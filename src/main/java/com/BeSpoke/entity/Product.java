package com.BeSpoke.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/** A vendor company's shop item. */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductCategory category;

    /** Optional room tag — reuses the wizard's ROOM_CHOICES values. */
    @Column(length = 60)
    private String roomType;

    /** The shop's browse rail — a SHOP_CATEGORY option value ("Sofas & seating"). */
    @Column(length = 120)
    private String shopCategory;

    /** The sub-type within it — a SHOP_SUBCATEGORY option value ("L-shaped sofa"). */
    @Column(length = 120)
    private String shopSubCategory;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal price;

    @Column(length = 1000)
    private String imageUrl;

    private Integer widthMm;
    private Integer depthMm;
    private Integer heightMm;
    @Column(length = 1000)
    private String modelUrl;
    @Column(length = 7)
    private String finishColor;

    public Integer getWidthMm() { return widthMm; }
    public Integer getDepthMm() { return depthMm; }
    public Integer getHeightMm() { return heightMm; }
    public String getModelUrl() { return modelUrl; }
    public String getFinishColor() { return finishColor; }
    public void setSpatialSpec(com.BeSpoke.dto.ProductSpatialSpec spec) {
        widthMm = spec.widthMm(); depthMm = spec.depthMm(); heightMm = spec.heightMm();
        modelUrl = spec.modelUrl(); finishColor = spec.finishColor();
    }

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Product() {
    }

    public Product(Company company, String name, ProductCategory category, BigDecimal price) {
        this.company = company;
        this.name = name;
        this.category = category;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public String getShopCategory() {
        return shopCategory;
    }

    public void setShopCategory(String shopCategory) {
        this.shopCategory = shopCategory;
    }

    public String getShopSubCategory() {
        return shopSubCategory;
    }

    public void setShopSubCategory(String shopSubCategory) {
        this.shopSubCategory = shopSubCategory;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
