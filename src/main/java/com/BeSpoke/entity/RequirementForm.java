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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** The Customer Requirement Form wizard: one per lead, autosaved as DRAFT, then SUBMITTED. */
@Entity
@Table(name = "requirement_forms")
public class RequirementForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "lead_id", nullable = false, unique = true)
    private Lead lead;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequirementFormStatus status = RequirementFormStatus.DRAFT;

    private Instant submittedAt;

    private Instant updatedAt = Instant.now();

    // Step 1 - Property & project
    private String projectSegment;
    private String spaceType;
    private String projectType;
    @Column(length = 1000)
    private String scopeOfWork;
    private String numberOfFloors;
    private Integer totalAreaSqft;
    private LocalDate desiredStartDate;
    private LocalDate desiredCompletionDate;
    private String occupancyStatus;
    @Column(length = 1000)
    private String renovationReason;

    // Step 2 - Household & lifestyle
    private Integer wfhMembers;
    private String guestFrequency;
    @Column(length = 500)
    private String accessibilityNeeds;
    @Column(length = 500)
    private String allergies;
    @Column(length = 500)
    private String petsKids;

    // Step 5 - Style & materials
    private String preferredStyle;
    private String secondaryStyle;
    @Column(length = 500)
    private String stylesToAvoid;
    @Column(length = 1000)
    private String inspirationLinks;
    @Column(length = 500)
    private String colourPalette;
    private String woodTone;
    private String metalFinish;
    @Column(length = 500)
    private String flooringPreference;
    @Column(length = 500)
    private String sustainabilityPreference;
    @Column(length = 500)
    private String materialsToAvoid;
    @Column(length = 500)
    private String localHandcraftedPreference;

    // Step 6 - Budget & timeline
    private String budgetRange;
    private String budgetFlexibility;
    private String paymentMilestonePreference;
    @Column(length = 500)
    private String clientSourcedItems;
    @Column(length = 500)
    private String priorityAreas;
    private LocalDate targetMoveInDate;
    @Column(length = 500)
    private String fixedDeadlines;
    @Column(length = 500)
    private String siteAccess;
    @Column(length = 500)
    private String societyApproval;
    @Column(length = 500)
    private String structuralChanges;
    @Column(length = 500)
    private String elevatorRestrictions;

    @OneToMany(mappedBy = "form", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<RequirementRoom> rooms = new ArrayList<>();

    public RequirementForm() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Lead getLead() {
        return lead;
    }

    public void setLead(Lead lead) {
        this.lead = lead;
    }

    public RequirementFormStatus getStatus() {
        return status;
    }

    public void setStatus(RequirementFormStatus status) {
        this.status = status;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getProjectSegment() {
        return projectSegment;
    }

    public void setProjectSegment(String projectSegment) {
        this.projectSegment = projectSegment;
    }

    public String getSpaceType() {
        return spaceType;
    }

    public void setSpaceType(String spaceType) {
        this.spaceType = spaceType;
    }

    public String getProjectType() {
        return projectType;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    public String getScopeOfWork() {
        return scopeOfWork;
    }

    public void setScopeOfWork(String scopeOfWork) {
        this.scopeOfWork = scopeOfWork;
    }

    public String getNumberOfFloors() {
        return numberOfFloors;
    }

    public void setNumberOfFloors(String numberOfFloors) {
        this.numberOfFloors = numberOfFloors;
    }

    public Integer getTotalAreaSqft() {
        return totalAreaSqft;
    }

    public void setTotalAreaSqft(Integer totalAreaSqft) {
        this.totalAreaSqft = totalAreaSqft;
    }

    public LocalDate getDesiredStartDate() {
        return desiredStartDate;
    }

    public void setDesiredStartDate(LocalDate desiredStartDate) {
        this.desiredStartDate = desiredStartDate;
    }

    public LocalDate getDesiredCompletionDate() {
        return desiredCompletionDate;
    }

    public void setDesiredCompletionDate(LocalDate desiredCompletionDate) {
        this.desiredCompletionDate = desiredCompletionDate;
    }

    public String getOccupancyStatus() {
        return occupancyStatus;
    }

    public void setOccupancyStatus(String occupancyStatus) {
        this.occupancyStatus = occupancyStatus;
    }

    public String getRenovationReason() {
        return renovationReason;
    }

    public void setRenovationReason(String renovationReason) {
        this.renovationReason = renovationReason;
    }

    public Integer getWfhMembers() {
        return wfhMembers;
    }

    public void setWfhMembers(Integer wfhMembers) {
        this.wfhMembers = wfhMembers;
    }

    public String getGuestFrequency() {
        return guestFrequency;
    }

    public void setGuestFrequency(String guestFrequency) {
        this.guestFrequency = guestFrequency;
    }

    public String getAccessibilityNeeds() {
        return accessibilityNeeds;
    }

    public void setAccessibilityNeeds(String accessibilityNeeds) {
        this.accessibilityNeeds = accessibilityNeeds;
    }

    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public String getPetsKids() {
        return petsKids;
    }

    public void setPetsKids(String petsKids) {
        this.petsKids = petsKids;
    }

    public String getPreferredStyle() {
        return preferredStyle;
    }

    public void setPreferredStyle(String preferredStyle) {
        this.preferredStyle = preferredStyle;
    }

    public String getSecondaryStyle() {
        return secondaryStyle;
    }

    public void setSecondaryStyle(String secondaryStyle) {
        this.secondaryStyle = secondaryStyle;
    }

    public String getStylesToAvoid() {
        return stylesToAvoid;
    }

    public void setStylesToAvoid(String stylesToAvoid) {
        this.stylesToAvoid = stylesToAvoid;
    }

    public String getInspirationLinks() {
        return inspirationLinks;
    }

    public void setInspirationLinks(String inspirationLinks) {
        this.inspirationLinks = inspirationLinks;
    }

    public String getColourPalette() {
        return colourPalette;
    }

    public void setColourPalette(String colourPalette) {
        this.colourPalette = colourPalette;
    }

    public String getWoodTone() {
        return woodTone;
    }

    public void setWoodTone(String woodTone) {
        this.woodTone = woodTone;
    }

    public String getMetalFinish() {
        return metalFinish;
    }

    public void setMetalFinish(String metalFinish) {
        this.metalFinish = metalFinish;
    }

    public String getFlooringPreference() {
        return flooringPreference;
    }

    public void setFlooringPreference(String flooringPreference) {
        this.flooringPreference = flooringPreference;
    }

    public String getSustainabilityPreference() {
        return sustainabilityPreference;
    }

    public void setSustainabilityPreference(String sustainabilityPreference) {
        this.sustainabilityPreference = sustainabilityPreference;
    }

    public String getMaterialsToAvoid() {
        return materialsToAvoid;
    }

    public void setMaterialsToAvoid(String materialsToAvoid) {
        this.materialsToAvoid = materialsToAvoid;
    }

    public String getLocalHandcraftedPreference() {
        return localHandcraftedPreference;
    }

    public void setLocalHandcraftedPreference(String localHandcraftedPreference) {
        this.localHandcraftedPreference = localHandcraftedPreference;
    }

    public String getBudgetRange() {
        return budgetRange;
    }

    public void setBudgetRange(String budgetRange) {
        this.budgetRange = budgetRange;
    }

    public String getBudgetFlexibility() {
        return budgetFlexibility;
    }

    public void setBudgetFlexibility(String budgetFlexibility) {
        this.budgetFlexibility = budgetFlexibility;
    }

    public String getPaymentMilestonePreference() {
        return paymentMilestonePreference;
    }

    public void setPaymentMilestonePreference(String paymentMilestonePreference) {
        this.paymentMilestonePreference = paymentMilestonePreference;
    }

    public String getClientSourcedItems() {
        return clientSourcedItems;
    }

    public void setClientSourcedItems(String clientSourcedItems) {
        this.clientSourcedItems = clientSourcedItems;
    }

    public String getPriorityAreas() {
        return priorityAreas;
    }

    public void setPriorityAreas(String priorityAreas) {
        this.priorityAreas = priorityAreas;
    }

    public LocalDate getTargetMoveInDate() {
        return targetMoveInDate;
    }

    public void setTargetMoveInDate(LocalDate targetMoveInDate) {
        this.targetMoveInDate = targetMoveInDate;
    }

    public String getFixedDeadlines() {
        return fixedDeadlines;
    }

    public void setFixedDeadlines(String fixedDeadlines) {
        this.fixedDeadlines = fixedDeadlines;
    }

    public String getSiteAccess() {
        return siteAccess;
    }

    public void setSiteAccess(String siteAccess) {
        this.siteAccess = siteAccess;
    }

    public String getSocietyApproval() {
        return societyApproval;
    }

    public void setSocietyApproval(String societyApproval) {
        this.societyApproval = societyApproval;
    }

    public String getStructuralChanges() {
        return structuralChanges;
    }

    public void setStructuralChanges(String structuralChanges) {
        this.structuralChanges = structuralChanges;
    }

    public String getElevatorRestrictions() {
        return elevatorRestrictions;
    }

    public void setElevatorRestrictions(String elevatorRestrictions) {
        this.elevatorRestrictions = elevatorRestrictions;
    }

    public List<RequirementRoom> getRooms() {
        return rooms;
    }

    public void setRooms(List<RequirementRoom> rooms) {
        this.rooms = rooms;
    }
}
