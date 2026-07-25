package com.BeSpoke.dto;

import com.BeSpoke.entity.Lead;

import java.time.Instant;
import java.time.LocalDate;

public record LeadSummaryDto(
        Long id,
        String contactName,
        String contactEmail,
        String contactPhone,
        String city,
        String propertyType,
        String budgetBand,
        String status,
        int score,
        String source,
        UserRefDto assignedDesigner,
        LocalDate followUpAt,
        Instant createdAt,
        String formStatus,
        boolean hasCustomerAccount
) {

    public static LeadSummaryDto from(Lead lead, String formStatus) {
        return new LeadSummaryDto(
                lead.getId(),
                lead.getContactName(),
                lead.getContactEmail(),
                lead.getContactPhone(),
                lead.getCity(),
                lead.getPropertyType(),
                lead.getBudgetBand(),
                lead.getStatus().name(),
                lead.getScore(),
                lead.getSource().name(),
                UserRefDto.from(lead.getAssignedDesigner()),
                lead.getFollowUpAt(),
                lead.getCreatedAt(),
                formStatus,
                lead.getCustomer() != null
        );
    }
}
