package com.BeSpoke.dto;

import com.BeSpoke.entity.Lead;

import java.math.BigDecimal;
import java.time.Instant;

public record LeadDto(
        Long id,
        String status,
        String category,
        UserDto customer,
        UserDto designer,
        Long orderId,
        BigDecimal amount,
        String contactName,
        String contactEmail,
        String contactPhone,
        String message,
        Instant createdAt,
        Instant updatedAt
) {

    public static LeadDto from(Lead lead) {
        return new LeadDto(
                lead.getId(),
                lead.getStatus().name(),
                lead.getCategory() != null ? lead.getCategory().name() : null,
                UserDto.from(lead.getCustomer()),
                UserDto.from(lead.getDesigner()),
                lead.getOrder() != null ? lead.getOrder().getId() : null,
                lead.getOrder() != null ? lead.getOrder().getTotalAmount() : null,
                lead.getContactName(),
                lead.getContactEmail(),
                lead.getContactPhone(),
                lead.getMessage(),
                lead.getCreatedAt(),
                lead.getUpdatedAt()
        );
    }
}
