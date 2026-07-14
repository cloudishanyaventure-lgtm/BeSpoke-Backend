package com.BeSpoke.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateLeadStatusRequest(
        @NotBlank @Pattern(regexp = "IN_PROGRESS|COMPLETED",
                message = "status must be IN_PROGRESS or COMPLETED") String status
) {
}
