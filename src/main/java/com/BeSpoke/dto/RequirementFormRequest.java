package com.BeSpoke.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Upsert of the requirement form's scalar fields (rooms are replaced via their own endpoint). */
public record RequirementFormRequest(
        @Size(max = 255) String projectSegment,
        @Size(max = 255) String spaceType,
        @Size(max = 255) String projectType,
        @Size(max = 1000) String scopeOfWork,
        @Size(max = 255) String numberOfFloors,
        Integer totalAreaSqft,
        LocalDate desiredStartDate,
        LocalDate desiredCompletionDate,
        @Size(max = 255) String occupancyStatus,
        @Size(max = 1000) String renovationReason,
        Integer wfhMembers,
        @Size(max = 255) String guestFrequency,
        @Size(max = 500) String accessibilityNeeds,
        @Size(max = 500) String allergies,
        @Size(max = 500) String petsKids,
        @Size(max = 255) String preferredStyle,
        @Size(max = 255) String secondaryStyle,
        @Size(max = 500) String stylesToAvoid,
        @Size(max = 1000) String inspirationLinks,
        @Size(max = 500) String colourPalette,
        @Size(max = 255) String woodTone,
        @Size(max = 255) String metalFinish,
        @Size(max = 500) String flooringPreference,
        @Size(max = 500) String sustainabilityPreference,
        @Size(max = 500) String materialsToAvoid,
        @Size(max = 500) String localHandcraftedPreference,
        @Size(max = 255) String budgetRange,
        @Size(max = 255) String budgetFlexibility,
        @Size(max = 255) String paymentMilestonePreference,
        @Size(max = 500) String clientSourcedItems,
        @Size(max = 500) String priorityAreas,
        LocalDate targetMoveInDate,
        @Size(max = 500) String fixedDeadlines,
        @Size(max = 500) String siteAccess,
        @Size(max = 500) String societyApproval,
        @Size(max = 500) String structuralChanges,
        @Size(max = 500) String elevatorRestrictions
) {
}
