package com.BeSpoke.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectDrawingRequest(@NotBlank @Size(max = 500) String reason) {
}
