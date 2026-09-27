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

import java.time.Instant;
import java.time.LocalDate;

/**
 * One element of the BOQ on the timeline: which room it belongs to, its own date slice, and
 * how the work against it is going. "Not started" / "Delayed" are derived from the dates and
 * this status at read time — only IN_PROGRESS and COMPLETED are recorded here.
 */
@Entity
@Table(name = "drawing_schedule_items")
public class DrawingScheduleItem {

    public enum Status { NOT_STARTED, IN_PROGRESS, COMPLETED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private DrawingSchedule schedule;

    /** The room this element sits under, denormalised so it survives a later BOQ edit. */
    @Column(length = 200)
    private String section;

    @Column(nullable = false, length = 500)
    private String label;

    @Column(nullable = false)
    private int orderIndex;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.NOT_STARTED;

    private Instant startedAt;

    private Instant completedAt;

    public DrawingScheduleItem() {
    }

    public Long getId() {
        return id;
    }

    public DrawingSchedule getSchedule() {
        return schedule;
    }

    public void setSchedule(DrawingSchedule schedule) {
        this.schedule = schedule;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
