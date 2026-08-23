package com.BeSpoke.dto;

import com.BeSpoke.entity.PolicyDocument;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/** A policy as the site renders it. `body` is omitted from the hub listing. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PolicyDto(Long id, String slug, String title, String summary,
                        String effectiveDate, String body, String linkPath,
                        int sortOrder, Boolean active, Instant updatedAt) {

    /** Hub card — no body, so the listing stays small. */
    public static PolicyDto card(PolicyDocument p) {
        return new PolicyDto(p.getId(), p.getSlug(), p.getTitle(), p.getSummary(),
                p.getEffectiveDate(), null, p.getLinkPath(), p.getSortOrder(), null,
                p.getUpdatedAt());
    }

    /** Full document, public page. */
    public static PolicyDto full(PolicyDocument p) {
        return new PolicyDto(p.getId(), p.getSlug(), p.getTitle(), p.getSummary(),
                p.getEffectiveDate(), p.getBody(), p.getLinkPath(), p.getSortOrder(), null,
                p.getUpdatedAt());
    }

    /** Admin view — includes the unpublished ones and their flag. */
    public static PolicyDto admin(PolicyDocument p) {
        return new PolicyDto(p.getId(), p.getSlug(), p.getTitle(), p.getSummary(),
                p.getEffectiveDate(), p.getBody(), p.getLinkPath(), p.getSortOrder(),
                p.isActive(), p.getUpdatedAt());
    }
}
