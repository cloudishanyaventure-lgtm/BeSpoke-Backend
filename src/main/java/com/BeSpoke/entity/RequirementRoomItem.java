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
}
