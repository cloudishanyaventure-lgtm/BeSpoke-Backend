package com.BeSpoke.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Seeded reference catalog: per space type, the categorised checklist items the wizard offers. */
@Entity
@Table(name = "room_catalog_items")
public class RoomCatalogItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String spaceType;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, length = 500)
    private String item;

    @Column(nullable = false)
    private int sortOrder;

    public RoomCatalogItem() {
    }

    public RoomCatalogItem(String spaceType, String category, String item, int sortOrder) {
        this.spaceType = spaceType;
        this.category = category;
        this.item = item;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSpaceType() {
        return spaceType;
    }

    public void setSpaceType(String spaceType) {
        this.spaceType = spaceType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
