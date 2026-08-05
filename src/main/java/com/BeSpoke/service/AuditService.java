package com.BeSpoke.service;

import com.BeSpoke.dto.AuditEventDto;
import com.BeSpoke.entity.AuditEvent;
import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.User;
import com.BeSpoke.repository.AuditEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/** Append-only trail: platform ↔ company actions, both sides read the same feed. */
@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public void log(User actor, Company company, String action, String detail) {
        auditEventRepository.save(new AuditEvent(company,
                actor != null ? actor.getName() : "System",
                actor != null ? actor.getRole() : null,
                action, detail));
    }

    /** company == null → platform-wide feed. Callers pass a pre-scoped company. */
    public List<AuditEventDto> list(Company company, int limit) {
        PageRequest page = PageRequest.of(0, Math.min(Math.max(limit, 1), 200));
        List<AuditEvent> events = company != null
                ? auditEventRepository.findByCompanyOrderByCreatedAtDesc(company, page)
                : auditEventRepository.findAllByOrderByCreatedAtDesc(page);
        return events.stream().map(AuditEventDto::from).toList();
    }
}
