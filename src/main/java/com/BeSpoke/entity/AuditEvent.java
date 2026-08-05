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

/**
 * Two-way visibility trail: platform actions on a company are visible to its
 * director and vice versa. company == null for platform-wide events.
 */
@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(length = 120)
    private String actorName;

    @Enumerated(EnumType.STRING)
    private Role actorRole;

    @Column(length = 60)
    private String action;

    @Column(length = 1000)
    private String detail;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public AuditEvent() {
    }

    public AuditEvent(Company company, String actorName, Role actorRole, String action, String detail) {
        this.company = company;
        this.actorName = actorName;
        this.actorRole = actorRole;
        this.action = action;
        this.detail = detail;
    }

    public Long getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public String getActorName() {
        return actorName;
    }

    public Role getActorRole() {
        return actorRole;
    }

    public String getAction() {
        return action;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
