package com.BeSpoke.dto;

import com.BeSpoke.entity.RequirementForm;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RequirementFormDto(
        Long id,
        Long leadId,
        String status,
        Instant submittedAt,
        Instant approvedAt,
        Instant updatedAt,
        // Property & project
        String projectSegment,
        String spaceType,
        String projectType,
        String scopeOfWork,
        String numberOfFloors,
        Integer totalAreaSqft,
        LocalDate desiredStartDate,
        LocalDate desiredCompletionDate,
        String occupancyStatus,
        String renovationReason,
        // Household & lifestyle
        Integer totalAdults,
        Integer seniorCitizens,
        String kidsDetails,
        Integer wfhMembers,
        String guestFrequency,
        String accessibilityNeeds,
        String allergies,
        String petsKids,
        // Style & materials
        String preferredStyle,
        String secondaryStyle,
        String stylesToAvoid,
        String inspirationLinks,
        String colourPalette,
        String woodTone,
        String metalFinish,
        String flooringPreference,
        String sustainabilityPreference,
        String materialsToAvoid,
        String localHandcraftedPreference,
        // Budget & timeline
        String budgetRange,
        String budgetFlexibility,
        String paymentMilestonePreference,
        String clientSourcedItems,
        String priorityAreas,
        LocalDate targetMoveInDate,
        String fixedDeadlines,
        String siteAccess,
        String societyApproval,
        String structuralChanges,
        String elevatorRestrictions,
        List<RoomDto> rooms
) {

    public static RequirementFormDto from(RequirementForm form) {
        return new RequirementFormDto(
                form.getId(),
                form.getLead().getId(),
                form.getStatus().name(),
                form.getSubmittedAt(),
                form.getApprovedAt(),
                form.getUpdatedAt(),
                form.getProjectSegment(),
                form.getSpaceType(),
                form.getProjectType(),
                form.getScopeOfWork(),
                form.getNumberOfFloors(),
                form.getTotalAreaSqft(),
                form.getDesiredStartDate(),
                form.getDesiredCompletionDate(),
                form.getOccupancyStatus(),
                form.getRenovationReason(),
                form.getTotalAdults(),
                form.getSeniorCitizens(),
                form.getKidsDetails(),
                form.getWfhMembers(),
                form.getGuestFrequency(),
                form.getAccessibilityNeeds(),
                form.getAllergies(),
                form.getPetsKids(),
                form.getPreferredStyle(),
                form.getSecondaryStyle(),
                form.getStylesToAvoid(),
                form.getInspirationLinks(),
                form.getColourPalette(),
                form.getWoodTone(),
                form.getMetalFinish(),
                form.getFlooringPreference(),
                form.getSustainabilityPreference(),
                form.getMaterialsToAvoid(),
                form.getLocalHandcraftedPreference(),
                form.getBudgetRange(),
                form.getBudgetFlexibility(),
                form.getPaymentMilestonePreference(),
                form.getClientSourcedItems(),
                form.getPriorityAreas(),
                form.getTargetMoveInDate(),
                form.getFixedDeadlines(),
                form.getSiteAccess(),
                form.getSocietyApproval(),
                form.getStructuralChanges(),
                form.getElevatorRestrictions(),
                form.getRooms().stream().map(RoomDto::from).toList()
        );
    }
}
