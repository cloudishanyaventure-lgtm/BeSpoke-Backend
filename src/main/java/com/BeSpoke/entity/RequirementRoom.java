package com.BeSpoke.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/** One room in a requirement form, e.g. "Master Bedroom" of type BEDROOM. */
@Entity
@Table(name = "requirement_rooms")
public class RequirementRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "form_id", nullable = false)
    private RequirementForm form;

    /** Room type key, e.g. BEDROOM / KITCHEN / LIVING_ROOM / BATHROOM / STUDY / TERRACE / DINING / BALCONY / OTHER. */
    @Column(nullable = false)
    private String roomType;

    /** Display name, e.g. "Master Bedroom". */
    @Column(nullable = false)
    private String label;

    /** Which floor the room is on, e.g. "Ground floor". */
    @Column(length = 60)
    private String floor;

    /** Family member the room is for, e.g. "Mrs. Sharma". */
    @Column(length = 120)
    private String familyMember;

    private String primaryUse;

    @Column(length = 1000)
    private String mustHaves;

    @Column(length = 500)
    private String reuseFurniture;

    /** LOW / MEDIUM / HIGH. */
    private String storageNeeds;

    private String colorPreference;

    @Column(length = 1000)
    private String specialRequirements;

    @Column(length = 1000)
    private String notes;

    /**
     * Room size in feet, captured when the space is added to the PRD. Nullable only for
     * rooms that predate the question — the form asks for all four and takes 0 for
     * "not applicable". A 0 is never multiplied into anything: see areaSqft().
     */
    private Double heightFt;
    private Double lengthFt;
    private Double widthFt;
    private Double depthFt;

    @Column(nullable = false)
    private int sortOrder;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<RequirementRoomItem> items = new ArrayList<>();

    public RequirementRoom() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RequirementForm getForm() {
        return form;
    }

    public void setForm(RequirementForm form) {
        this.form = form;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public String getFamilyMember() {
        return familyMember;
    }

    public void setFamilyMember(String familyMember) {
        this.familyMember = familyMember;
    }

    public String getPrimaryUse() {
        return primaryUse;
    }

    public void setPrimaryUse(String primaryUse) {
        this.primaryUse = primaryUse;
    }

    public String getMustHaves() {
        return mustHaves;
    }

    public void setMustHaves(String mustHaves) {
        this.mustHaves = mustHaves;
    }

    public String getReuseFurniture() {
        return reuseFurniture;
    }

    public void setReuseFurniture(String reuseFurniture) {
        this.reuseFurniture = reuseFurniture;
    }

    public String getStorageNeeds() {
        return storageNeeds;
    }

    public void setStorageNeeds(String storageNeeds) {
        this.storageNeeds = storageNeeds;
    }

    public String getColorPreference() {
        return colorPreference;
    }

    public void setColorPreference(String colorPreference) {
        this.colorPreference = colorPreference;
    }

    public String getSpecialRequirements() {
        return specialRequirements;
    }

    public void setSpecialRequirements(String specialRequirements) {
        this.specialRequirements = specialRequirements;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Double getHeightFt() {
        return heightFt;
    }

    public void setHeightFt(Double heightFt) {
        this.heightFt = heightFt;
    }

    public Double getLengthFt() {
        return lengthFt;
    }

    public void setLengthFt(Double lengthFt) {
        this.lengthFt = lengthFt;
    }

    public Double getWidthFt() {
        return widthFt;
    }

    public void setWidthFt(Double widthFt) {
        this.widthFt = widthFt;
    }

    public Double getDepthFt() {
        return depthFt;
    }

    public void setDepthFt(Double depthFt) {
        this.depthFt = depthFt;
    }

    /**
     * Floor area, or null when it cannot be worked out. A dimension left at 0 means "not
     * applicable here" — it is not a measurement, so it never multiplies an area down to
     * nothing. Width falls back to depth for spaces measured that way.
     */
    public Double areaSqft() {
        double length = lengthFt == null ? 0 : lengthFt;
        double across = widthFt != null && widthFt > 0 ? widthFt : (depthFt == null ? 0 : depthFt);
        return length > 0 && across > 0 ? length * across : null;
    }

    public List<RequirementRoomItem> getItems() {
        return items;
    }

    public void setItems(List<RequirementRoomItem> items) {
        this.items = items;
    }
}
