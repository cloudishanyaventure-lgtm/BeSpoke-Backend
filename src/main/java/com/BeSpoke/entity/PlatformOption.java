package com.BeSpoke.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * One entry in an editable picklist — cities, design styles, budget bands, the
 * "how it works" steps. Everything the UI offers as a choice lives here so the
 * platform admin can change it without a deploy; {@code listKey} names the list.
 */
@Entity
@Table(name = "platform_options",
        indexes = @Index(name = "idx_platform_options_list", columnList = "listKey"),
        uniqueConstraints = @UniqueConstraint(name = "uk_platform_option",
                columnNames = {"listKey", "value"}))
public class PlatformOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String listKey;

    /** What gets stored on the record — often the same text as the label. */
    @Column(nullable = false, length = 160)
    private String value;

    @Column(nullable = false, length = 255)
    private String label;

    /** Second line: the step's body copy, or a room's catalog key. */
    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active = true;

    public PlatformOption() {
    }

    public PlatformOption(String listKey, String value, String label, String note, int sortOrder) {
        this.listKey = listKey;
        this.value = value;
        this.label = label;
        this.note = note;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getListKey() {
        return listKey;
    }

    public void setListKey(String listKey) {
        this.listKey = listKey;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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
}
