package com.BeSpoke.entity;
import jakarta.persistence.*;
import java.time.Instant;

/** A customer's room concept. Lead scoping controls the studio's read access. */
@Entity
@Table(name = "room_plans", indexes = @Index(name = "room_plan_owner_updated", columnList = "owner_id,updated_at"))
public class RoomPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version @Column(nullable = false) private Long version;
    @ManyToOne(optional = false) @JoinColumn(name = "owner_id", nullable = false) private User owner;
    @ManyToOne(optional = false) @JoinColumn(name = "lead_id", nullable = false) private Lead lead;
    @Column(nullable = false) private Instant createdAt = Instant.now();
    @Column(nullable = false) private Instant updatedAt = Instant.now();
    @Column(nullable = false, length = 60) private String name;
    @Column(nullable = false) private Integer wall;
    @Column(nullable = false) private Integer floor;
    @Column(nullable = false) private Integer fabric;
    @Column(nullable = false) private Double width;
    @Column(nullable = false) private Double depth;
    @Column(nullable = false, length = 20) private String layout;
    @Column(nullable = false) private Boolean rug;
    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Long getVersion() { return version; }
    public void setVersion(Long value) { version = value; }
    public User getOwner() { return owner; }
    public void setOwner(User value) { owner = value; }
    public Lead getLead() { return lead; }
    public void setLead(Lead value) { lead = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { createdAt = value; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant value) { updatedAt = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public Integer getWall() { return wall; }
    public void setWall(Integer value) { wall = value; }
    public Integer getFloor() { return floor; }
    public void setFloor(Integer value) { floor = value; }
    public Integer getFabric() { return fabric; }
    public void setFabric(Integer value) { fabric = value; }
    public Double getWidth() { return width; }
    public void setWidth(Double value) { width = value; }
    public Double getDepth() { return depth; }
    public void setDepth(Double value) { depth = value; }
    public String getLayout() { return layout; }
    public void setLayout(String value) { layout = value; }
    public Boolean getRug() { return rug; }
    public void setRug(Boolean value) { rug = value; }
}
