package com.BeSpoke.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Create or update a policy document. Slug is derived from the title when blank. */
public record PolicyRequest(
        @Size(max = 120) String slug,
        @NotBlank @Size(max = 255) String title,
        @Size(max = 500) String summary,
        @Size(max = 120) String effectiveDate,
        String body,
        @Size(max = 255) String linkPath,
        Integer sortOrder,
        Boolean active
) {
}
