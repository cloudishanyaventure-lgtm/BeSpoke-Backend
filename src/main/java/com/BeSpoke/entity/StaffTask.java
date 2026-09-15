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
 * A piece of work one person in a company hands to another. Deliberately not tied to a
 * lead, a project or a quote: the ask was "tasks can be anything", and every one of those
 * already has its own activity trail. All this carries is who, what and by when.
 *
 * <p>Named StaffTask rather than Task because {@code java.util.concurrent} and Gradle both
 * own a {@code Task} and the import clashes are not worth the shorter name.
 */
@Entity
@Table(name = "staff_tasks")
public class StaffTask {

    public enum Status { OPEN, IN_PROGRESS, BLOCKED, DONE }
    public enum Visibility { PUBLIC, PRIVATE }
    /** LOW/NORMAL/HIGH are legacy values kept so old rows still read; the form offers the last four. */
    public enum Priority { LOW, NORMAL, HIGH, URGENT, ESCALATION, SITE_VISIT, COMPLETE_BY_TODAY }

    // Nullable for existing installations: historical work must stay private.
    @Enumerated(EnumType.STRING)
    private Visibility visibility;
    @Enumerated(EnumType.STRING)
    private Priority priority;

    public Visibility getVisibility() { return visibility == null ? Visibility.PRIVATE : visibility; }
    public void setVisibility(Visibility value) { visibility = value; }
    public Priority getPriority() { return priority == null ? Priority.NORMAL : priority; }
    public void setPriority(Priority value) { priority = value; }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Scope. Every read is filtered by this — a task never crosses a tenant. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String details;

    /** The person tagged. Always staff of {@link #company}. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "assignee_id", nullable = false)
    private User assignee;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    /** Optional: the lead this work is about, picked in the brief. */
    @ManyToOne
    @JoinColumn(name = "lead_id")
    private Lead lead;

    /** Optional: the customer this work is about, picked in the brief. */
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private User customer;

    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.OPEN;

    private Instant completedAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public StaffTask() {
    }

    public StaffTask(Company company, String title, User assignee, User createdBy) {
        this.company = company;
        this.title = title;
        this.assignee = assignee;
        this.createdBy = createdBy;
    }

    public Long getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public Lead getLead() {
        return lead;
    }

    public void setLead(Lead lead) {
        this.lead = lead;
    }

    public User getCustomer() {
        return customer;
    }

    public void setCustomer(User customer) {
        this.customer = customer;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Status getStatus() {
        return status != null ? status : Status.OPEN;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
