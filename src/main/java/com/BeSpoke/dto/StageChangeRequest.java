package com.BeSpoke.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StageChangeRequest(
        @NotBlank String stage,
        @Size(max = 1000) String reason
) {
}
