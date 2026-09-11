package com.BeSpoke.entity;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import java.time.Instant;

/** Immutable content; sharing and customer decisions are version-specific. */
@Entity
@Table(name="project_documents", indexes=@Index(name="document_lead_created", columnList="lead_id,createdAt"))
public class ProjectDocument {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional=false) public Lead lead;
    @ManyToOne(optional=false) public User uploadedBy;
    @Column(nullable=false,length=200) public String name;
    @Column(nullable=false,length=30) public String type;
    @Column(nullable=false,length=80) public String contentType;
    @Column(nullable=false) public long size;
    @Column(nullable=false,length=64) public String sha256;
    @Column(nullable=false,length=30) public String status = "DRAFT";
    @Column(nullable=false) public int revisionNumber = 1;
    @ManyToOne @JoinColumn(unique=true) public ProjectDocument previousRevision;
    public Instant supersededAt;
    @ManyToOne public User decidedBy;
    public Instant decidedAt;
    @Column(length=1000) public String feedback;
    @Column(nullable=false) public Instant createdAt = Instant.now();
    @Version @ColumnDefault("0") public long rowVersion;
}
