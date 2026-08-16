package com.BeSpoke.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A manufacturer in the library. Brands span categories — Greenlam sells laminates,
 * veneers and flooring — so the link is many-to-many rather than a column.
 */
@Entity
@Table(name = "material_brands")
public class MaterialBrand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String note;

    @Column(length = 1000)
    private String imageUrl;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "material_brand_categories",
            joinColumns = @JoinColumn(name = "brand_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<MaterialCategory> categories = new LinkedHashSet<>();

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() {
        return id;
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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Set<MaterialCategory> getCategories() {
        return categories;
    }

    public void setCategories(Set<MaterialCategory> categories) {
        this.categories = categories;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
