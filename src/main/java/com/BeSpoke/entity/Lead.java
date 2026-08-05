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
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A sales lead in the DesignConnect funnel. Created atomically at customer
 * signup (source WEBSITE), or manually by admins / the public enquiry form
 * for walk-in-style leads that have no user account yet.
 */
@Entity
@Table(name = "leads")
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Registered customer - nullable for walk-in / enquiry leads. */
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private User customer;

    /** Studio working this lead. Null = unrouted; only the platform admin sees it. */
    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    /**
     * Studio the customer asked for at signup / enquiry. A preference only: every lead
     * starts in the BeSpoke pool and the platform routes it (V3 §0).
     */
    @ManyToOne
    @JoinColumn(name = "preferred_company_id")
    private Company preferredCompany;

    @Column(nullable = false)
    private String contactName;

    @Column(nullable = false)
    private String contactEmail;

    @Column(nullable = false)
    private String contactPhone;

    @Column(nullable = false)
    private String city;

    private String propertyType;

    private String budgetBand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadStatus status;

    @Column(nullable = false)
    private int score;

    @ManyToOne
    @JoinColumn(name = "assigned_designer_id")
    private User assignedDesigner;

    /** Consultant / sales executive owning the customer relationship. */
    @ManyToOne
    @JoinColumn(name = "sales_owner_id")
    private User salesOwner;

    /** When BeSpoke transferred the lead to its current company. */
    private Instant transferredAt;

    /** When the company accepted the transferred lead. */
    private Instant acceptedAt;

    @ManyToOne
    @JoinColumn(name = "accepted_by_id")
    private User acceptedBy;

    private LocalDate followUpAt;

    private Instant wonAt;

    /** True while a designer/design-manager-captured lead awaits a senior's sign-off. */
    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean approvalPending = false;

    @Column(length = 120)
    private String createdByName;

    @Column(length = 40)
    private String createdByRole;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    public Lead() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getCustomer() {
        return customer;
    }

    public void setCustomer(User customer) {
        this.customer = customer;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public Company getPreferredCompany() {
        return preferredCompany;
    }

    public void setPreferredCompany(Company preferredCompany) {
        this.preferredCompany = preferredCompany;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(String propertyType) {
        this.propertyType = propertyType;
    }

    public String getBudgetBand() {
        return budgetBand;
    }

    public void setBudgetBand(String budgetBand) {
        this.budgetBand = budgetBand;
    }

    public LeadSource getSource() {
        return source;
    }

    public void setSource(LeadSource source) {
        this.source = source;
    }

    public LeadStatus getStatus() {
        return status;
    }

    public void setStatus(LeadStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public User getAssignedDesigner() {
        return assignedDesigner;
    }

    public void setAssignedDesigner(User assignedDesigner) {
        this.assignedDesigner = assignedDesigner;
    }

    public User getSalesOwner() {
        return salesOwner;
    }

    public void setSalesOwner(User salesOwner) {
        this.salesOwner = salesOwner;
    }

    public Instant getTransferredAt() {
        return transferredAt;
    }

    public void setTransferredAt(Instant transferredAt) {
        this.transferredAt = transferredAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Instant acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public User getAcceptedBy() {
        return acceptedBy;
    }

    public void setAcceptedBy(User acceptedBy) {
        this.acceptedBy = acceptedBy;
    }

    public LocalDate getFollowUpAt() {
        return followUpAt;
    }

    public void setFollowUpAt(LocalDate followUpAt) {
        this.followUpAt = followUpAt;
    }

    public Instant getWonAt() {
        return wonAt;
    }

    public void setWonAt(Instant wonAt) {
        this.wonAt = wonAt;
    }

    public boolean isApprovalPending() {
        return approvalPending;
    }

    public void setApprovalPending(boolean approvalPending) {
        this.approvalPending = approvalPending;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public String getCreatedByRole() {
        return createdByRole;
    }

    public void setCreatedByRole(String createdByRole) {
        this.createdByRole = createdByRole;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
