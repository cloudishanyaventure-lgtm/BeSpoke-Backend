package com.BeSpoke.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A checklist item the customer selected for a room. Only selected items are stored. */
@Entity
@Table(name = "requirement_room_items")
public class RequirementRoomItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private RequirementRoom room;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, length = 500)
    private String item;

    @Column(length = 500)
    private String note;

    /**
     * The element's own size in feet, captured beside the room's. Same rules as
     * {@link RequirementRoom}: nullable for items that predate the question, and a 0 is
     * "not applicable" — never multiplied into an area. See {@link #areaSqft()}.
     */
    private Double lengthFt;
    private Double widthFt;
    private Double depthFt;
    private Double heightFt;

    public RequirementRoomItem() {
    }

    public RequirementRoomItem(RequirementRoom room, String category, String item, String note) {
        this.room = room;
        this.category = category;
        this.item = item;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RequirementRoom getRoom() {
        return room;
    }

    public void setRoom(RequirementRoom room) {
        this.room = room;
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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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

    public Double getHeightFt() {
        return heightFt;
    }

    public void setHeightFt(Double heightFt) {
        this.heightFt = heightFt;
    }

    /** Length × width (or depth when width is 0), or null when it cannot be worked out. */
    public Double areaSqft() {
        double length = lengthFt == null ? 0 : lengthFt;
        double across = widthFt != null && widthFt > 0 ? widthFt : (depthFt == null ? 0 : depthFt);
        return length > 0 && across > 0 ? length * across : null;
    }
}
