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
        boolean hasCustomerAccount,
        Long companyId,
        String companyName,
        UserRefDto salesOwner,
        Instant transferredAt,
        Instant acceptedAt,
        // Studio the customer asked for at signup — the pool page honours it in one click.
        Long preferredCompanyId,
        String preferredCompanyName,
        // Designer-captured leads await a senior's sign-off.
        boolean approvalPending,
        String createdByName,
        String createdByRole
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
                lead.getCustomer() != null,
                lead.getCompany() != null ? lead.getCompany().getId() : null,
                lead.getCompany() != null ? lead.getCompany().getName() : null,
                UserRefDto.from(lead.getSalesOwner()),
                lead.getTransferredAt(),
                lead.getAcceptedAt(),
                lead.getPreferredCompany() != null ? lead.getPreferredCompany().getId() : null,
                lead.getPreferredCompany() != null ? lead.getPreferredCompany().getName() : null,
                lead.isApprovalPending(),
                lead.getCreatedByName(),
                lead.getCreatedByRole()
        );
    }
}
