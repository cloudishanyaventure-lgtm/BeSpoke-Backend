package com.BeSpoke.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;
@Entity @Table(name="project_entries", indexes=@Index(name="entry_lead_created",columnList="lead_id,createdAt"))
public class ProjectEntry {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional=false) public Lead lead;
    @ManyToOne(optional=false) public User createdBy;
    @ManyToOne public User assignedTo;
    @Column(nullable=false,length=40) public String type;
    @Column(nullable=false,length=200) public String title;
    @Column(nullable=false,length=4000) public String details;
    @Column(nullable=false,length=30) public String status="OPEN";
    @Column(nullable=false) public boolean customerVisible;
    public Instant scheduledAt;
    public Instant dueAt;
    @Column(precision=14,scale=2) public BigDecimal amount;
    @Column(length=1000) public String resolution;
    @ManyToOne public User decidedBy;
    public Instant decidedAt;
    public Instant completedAt;
    @Column(nullable=false) public Instant createdAt=Instant.now();
    @Column(nullable=false) public Instant updatedAt=Instant.now();
    @Version public long version;
}
