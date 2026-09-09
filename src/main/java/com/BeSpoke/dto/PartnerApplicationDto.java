package com.BeSpoke.dto;

import com.BeSpoke.entity.PartnerApplication;

import java.time.Instant;

/** An application as the admin queue sees it. */
public record PartnerApplicationDto(
        Long id,
        String companyName,
        String city,
        String contactName,
        String contactEmail,
        String contactPhone,
        String status,
        String decisionNote,
        Long companyId,
        String decidedBy,
        Instant decidedAt,
        Instant createdAt
) {
    public static PartnerApplicationDto from(PartnerApplication application) {
        return new PartnerApplicationDto(
                application.getId(),
                application.getCompanyName(),
                application.getCity(),
                application.getContactName(),
                application.getContactEmail(),
                application.getContactPhone(),
                application.getStatus().name(),
                application.getDecisionNote(),
                application.getCompany() != null ? application.getCompany().getId() : null,
                application.getDecidedBy() != null ? application.getDecidedBy().getName() : null,
                application.getDecidedAt(),
                application.getCreatedAt());
    }
}
