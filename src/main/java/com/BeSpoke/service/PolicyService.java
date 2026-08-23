package com.BeSpoke.service;

import com.BeSpoke.dto.PolicyDto;
import com.BeSpoke.dto.PolicyRequest;
import com.BeSpoke.entity.PolicyDocument;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.PolicyDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/** The published legal documents behind /policies, editable at /admin/policies. */
@Service
@Transactional(readOnly = true)
public class PolicyService {

    private final PolicyDocumentRepository repository;
    private final AuditService auditService;

    public PolicyService(PolicyDocumentRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    public List<PolicyDto> published() {
        return repository.findByActiveTrueOrderBySortOrderAsc().stream().map(PolicyDto::card).toList();
    }

    public PolicyDto publicBySlug(String slug) {
        return repository.findBySlug(slug)
                .filter(PolicyDocument::isActive)
                .map(PolicyDto::full)
                .orElseThrow(() -> new NotFoundException("Policy not found"));
    }

    public List<PolicyDto> all() {
        return repository.findAllByOrderBySortOrderAsc().stream().map(PolicyDto::admin).toList();
    }

    @Transactional
    public PolicyDto create(User actor, PolicyRequest request) {
        String slug = slugOf(request);
        if (repository.existsBySlug(slug)) {
            throw new BadRequestException("A policy with the address /" + slug + " already exists");
        }
        PolicyDocument policy = new PolicyDocument(slug, request.title().trim(),
                trimToNull(request.summary()), trimToNull(request.effectiveDate()),
                request.body(), trimToNull(request.linkPath()),
                request.sortOrder() != null ? request.sortOrder() : nextSortOrder());
        if (request.active() != null) {
            policy.setActive(request.active());
        }
        policy = repository.save(policy);
        auditService.log(actor, null, "POLICY_CREATED", "Published policy \"" + policy.getTitle() + "\"");
        return PolicyDto.admin(policy);
    }

    @Transactional
    public PolicyDto update(User actor, Long id, PolicyRequest request) {
        PolicyDocument policy = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Policy not found"));
        String slug = slugOf(request);
        repository.findBySlug(slug).filter(other -> !other.getId().equals(id)).ifPresent(other -> {
            throw new BadRequestException("A policy with the address /" + slug + " already exists");
        });
        policy.setSlug(slug);
        policy.setTitle(request.title().trim());
        policy.setSummary(trimToNull(request.summary()));
        policy.setEffectiveDate(trimToNull(request.effectiveDate()));
        policy.setBody(request.body());
        policy.setLinkPath(trimToNull(request.linkPath()));
        if (request.sortOrder() != null) {
            policy.setSortOrder(request.sortOrder());
        }
        if (request.active() != null) {
            policy.setActive(request.active());
        }
        policy = repository.save(policy);
        auditService.log(actor, null, "POLICY_UPDATED", "Updated policy \"" + policy.getTitle() + "\"");
        return PolicyDto.admin(policy);
    }

    @Transactional
    public void delete(User actor, Long id) {
        PolicyDocument policy = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Policy not found"));
        repository.delete(policy);
        auditService.log(actor, null, "POLICY_DELETED", "Deleted policy \"" + policy.getTitle() + "\"");
    }

    private int nextSortOrder() {
        return repository.findAllByOrderBySortOrderAsc().stream()
                .mapToInt(PolicyDocument::getSortOrder).max().orElse(-1) + 1;
    }

    private static String slugOf(PolicyRequest request) {
        String raw = request.slug() != null && !request.slug().isBlank()
                ? request.slug() : request.title();
        String slug = raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (slug.isEmpty()) {
            throw new BadRequestException("That title cannot be turned into a web address");
        }
        return slug;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Publishes a shipped policy only if that slug has never existed. */
    @Transactional
    public int seedDefaults() {
        int added = 0;
        for (PolicyDocument policy : PolicyDefaults.DOCUMENTS) {
            if (repository.existsBySlug(policy.getSlug())) {
                continue;
            }
            repository.save(policy);
            added++;
        }
        return added;
    }
}
