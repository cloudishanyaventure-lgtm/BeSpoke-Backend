package com.BeSpoke.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record KycStatusRequest(
        @NotBlank @Pattern(regexp = "PENDING|VERIFIED|REJECTED",
                message = "status must be PENDING, VERIFIED or REJECTED") String status
) {
}
