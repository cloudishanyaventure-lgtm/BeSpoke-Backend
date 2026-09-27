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
 * A family member sharing one customer's project. The project is the {@link Lead}; its
 * {@code customer} is the owner, and every member here sees and acts on exactly what the
 * owner does. Only the owner manages this list — see {@code ProjectMemberService}.
 */
@Entity
@Table(name = "project_members")
public class ProjectMember {

    public enum Status { INVITED, JOINED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The shared project. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    /** The member's account. Null while the invite is still outstanding. */
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    /** Lower-cased email the invite went to; how an accept binds to (or creates) the account. */
    @Column(nullable = false, length = 255)
    private String invitedEmail;

    /** Name the owner gave them — becomes the account name on accept. */
    @Column(length = 160)
    private String invitedName;

    /** Phone the owner gave them — copied to the account on accept. */
    @Column(length = 20)
    private String invitedPhone;

    /** Single-use token in the invite link. Cleared once accepted. */
    @Column(length = 64)
    private String inviteToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.INVITED;

    @ManyToOne
    @JoinColumn(name = "invited_by_id")
    private User invitedBy;

    /**
     * Which room is "theirs" — the {@link RequirementRoom#getFamilyMember()} value they
     * picked on first entry. Only highlights their room; it never narrows their access.
     */
    @Column(length = 120)
    private String familyMember;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant joinedAt;

    public ProjectMember() {
    }

    public ProjectMember(Lead lead, String invitedEmail, String inviteToken, User invitedBy) {
        this.lead = lead;
        this.invitedEmail = invitedEmail;
        this.inviteToken = inviteToken;
        this.invitedBy = invitedBy;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getInvitedEmail() {
        return invitedEmail;
    }

    public void setInvitedEmail(String invitedEmail) {
        this.invitedEmail = invitedEmail;
    }

    public String getInvitedName() {
        return invitedName;
    }

    public void setInvitedName(String invitedName) {
        this.invitedName = invitedName;
    }

    public String getInvitedPhone() {
        return invitedPhone;
    }

    public void setInvitedPhone(String invitedPhone) {
        this.invitedPhone = invitedPhone;
    }

    public String getInviteToken() {
        return inviteToken;
    }

    public void setInviteToken(String inviteToken) {
        this.inviteToken = inviteToken;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public User getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(User invitedBy) {
        this.invitedBy = invitedBy;
    }

    public String getFamilyMember() {
        return familyMember;
    }

    public void setFamilyMember(String familyMember) {
        this.familyMember = familyMember;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }
}
