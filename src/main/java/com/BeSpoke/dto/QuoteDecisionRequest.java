package com.BeSpoke.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record QuoteDecisionRequest(
        @NotBlank @Pattern(regexp = "APPROVED|CHANGES_REQUESTED",
                message = "decision must be APPROVED or CHANGES_REQUESTED") String decision,
        @Size(max = 1000) String comment
) {
}
