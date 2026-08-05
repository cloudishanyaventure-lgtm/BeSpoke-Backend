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

/** A design drawing on a lead, moving through WIP → pending → approved → final. */
@Entity
@Table(name = "drawings")
public class Drawing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 60)
    private String floorLabel;

    @Column(length = 120)
    private String spaceLabel;

    /**
     * The PRD room this drawing belongs to. A plain id, not a relation, on purpose:
     * RequirementService.applyRooms deletes and recreates every room on each brief edit,
     * so a real FK would either block the edit or cascade the drawing away. The trade-off
     * is that this id goes stale after a brief edit — floorLabel/spaceLabel are stored
     * denormalised so the drawing still reads correctly when that happens.
     */
    private Long requirementRoomId;

    @Column(nullable = false, length = 1000)
    private String fileUrl;

    @Column(length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DrawingStatus status = DrawingStatus.WIP;

    @Column(length = 120)
    private String uploadedByName;

    private Instant submittedAt;

    @Column(length = 120)
    private String approvedByName;

    private Instant approvedAt;

    /** When the customer signed off (APPROVED → FINAL). */
    private Instant customerApprovedAt;

    @Column(length = 500)
    private String rejectionReason;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Drawing() {
    }

    public Drawing(Lead lead, String title, String fileUrl, String uploadedByName) {
        this.lead = lead;
        this.title = title;
        this.fileUrl = fileUrl;
        this.uploadedByName = uploadedByName;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFloorLabel() {
        return floorLabel;
    }

    public void setFloorLabel(String floorLabel) {
        this.floorLabel = floorLabel;
    }

    public String getSpaceLabel() {
        return spaceLabel;
    }

    public void setSpaceLabel(String spaceLabel) {
        this.spaceLabel = spaceLabel;
    }

    public Long getRequirementRoomId() {
        return requirementRoomId;
    }

    public void setRequirementRoomId(Long requirementRoomId) {
        this.requirementRoomId = requirementRoomId;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public DrawingStatus getStatus() {
        return status;
    }

    public void setStatus(DrawingStatus status) {
        this.status = status;
    }

    public String getUploadedByName() {
        return uploadedByName;
    }

    public void setUploadedByName(String uploadedByName) {
        this.uploadedByName = uploadedByName;
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

    public Instant getCustomerApprovedAt() {
        return customerApprovedAt;
    }

    public void setCustomerApprovedAt(Instant customerApprovedAt) {
        this.customerApprovedAt = customerApprovedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
