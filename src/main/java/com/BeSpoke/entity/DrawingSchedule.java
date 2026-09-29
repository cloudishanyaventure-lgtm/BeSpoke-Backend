package com.BeSpoke.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A project timeline: a start/end window for the whole BOQ, with each BOQ element given a
 * slice of that window. Built from the approved quote, adjusted by the studio, then signed
 * off by a design manager or director before work is tracked against it.
 *
 * <p>{@link Kind} is which timeline it is — the drawings one and the site execution one are
 * the same shape off the same BOQ, so a lead has one row per kind, not one row.
 */
@Entity
@Table(name = "drawing_schedules")
public class DrawingSchedule {

    public enum Status { DRAFT, SUBMITTED, APPROVED }

    /** DRAWING = the drawing schedule tab; EXECUTION = the site execution one. */
    public enum Kind { DRAWING, EXECUTION }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    // Nullable in the schema on purpose: ddl-auto adds the column to a table that already
    // has drawing rows, and the migration backfills them to DRAWING right after.
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Kind kind = Kind.DRAWING;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.DRAFT;

    @Column(length = 120)
    private String submittedByName;

    private Instant submittedAt;

    @Column(length = 120)
    private String approvedByName;

    private Instant approvedAt;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<DrawingScheduleItem> items = new ArrayList<>();

    public DrawingSchedule() {
    }

    public DrawingSchedule(Lead lead, Kind kind, LocalDate startDate, LocalDate endDate) {
        this.lead = lead;
        this.kind = kind;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public Long getId() {
        return id;
    }

    public Lead getLead() {
        return lead;
    }

    public void setLead(Lead lead) {
        this.lead = lead;
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

    public String getSubmittedByName() {
        return submittedByName;
    }

    public void setSubmittedByName(String submittedByName) {
        this.submittedByName = submittedByName;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getApprovedByName() {
        return approvedByName;
    }

    public void setApprovedByName(String approvedByName) {
        this.approvedByName = approvedByName;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public List<DrawingScheduleItem> getItems() {
        return items;
    }

    public void setItems(List<DrawingScheduleItem> items) {
        this.items = items;
    }
}
