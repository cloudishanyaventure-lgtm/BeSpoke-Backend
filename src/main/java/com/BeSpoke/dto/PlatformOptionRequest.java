package com.BeSpoke.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Create/update one picklist entry. `value` defaults to the label when omitted. */
public record PlatformOptionRequest(
        @NotBlank @Size(max = 60) String listKey,
        @Size(max = 160) String value,
        @NotBlank @Size(max = 255) String label,
        @Size(max = 500) String note,
        Integer sortOrder,
        Boolean active
) {
}
