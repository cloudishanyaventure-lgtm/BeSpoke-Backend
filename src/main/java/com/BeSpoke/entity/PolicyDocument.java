package com.BeSpoke.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A published legal document — cancellation policy, terms, privacy. Body is the
 * light markup the policy renderer understands (## / ### headings, "- " bullets,
 * "| a | b |" table rows, blank-line paragraphs; URLs and emails auto-link).
 * Edited at /admin/policies so the copy never needs a deploy.
 */
@Entity
@Table(name = "policy_documents")
public class PolicyDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(nullable = false, length = 255)
    private String title;

    /** One line under the title on the policies hub. */
    @Column(length = 500)
    private String summary;

    /** Free text, e.g. "20 August 2026" — printed under the title. */
    @Column(length = 120)
    private String effectiveDate;

    @Column(columnDefinition = "text")
    private String body;

    /**
     * Set when the document lives on its own page already (Terms of Use). The hub
     * links here instead of /policies/{slug} and the body is ignored.
     */
    @Column(length = 255)
    private String linkPath;

    @Column(nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public PolicyDocument() {
    }

    public PolicyDocument(String slug, String title, String summary, String effectiveDate,
                          String body, String linkPath, int sortOrder) {
        this.slug = slug;
        this.title = title;
        this.summary = summary;
        this.effectiveDate = effectiveDate;
        this.body = body;
        this.linkPath = linkPath;
        this.sortOrder = sortOrder;
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(String effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getLinkPath() {
        return linkPath;
    }

    public void setLinkPath(String linkPath) {
        this.linkPath = linkPath;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
