package com.BeSpoke.dto;

import com.BeSpoke.entity.AuditEvent;

import java.time.Instant;

public record AuditEventDto(Long id, Long companyId, String companyName, String actorName,
                            String actorRole, String action, String detail, Instant createdAt) {

    public static AuditEventDto from(AuditEvent event) {
        return new AuditEventDto(event.getId(),
                event.getCompany() != null ? event.getCompany().getId() : null,
                event.getCompany() != null ? event.getCompany().getName() : null,
                event.getActorName(),
                event.getActorRole() != null ? event.getActorRole().name() : null,
                event.getAction(), event.getDetail(), event.getCreatedAt());
    }
}
