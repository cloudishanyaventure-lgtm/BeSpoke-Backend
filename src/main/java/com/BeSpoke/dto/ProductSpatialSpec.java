package com.BeSpoke.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Physical envelope in millimetres. An empty model URL explicitly removes an old asset. */
public record ProductSpatialSpec(
        @NotNull @Min(1) @Max(50000) Integer widthMm,
        @NotNull @Min(1) @Max(50000) Integer depthMm,
        @NotNull @Min(1) @Max(50000) Integer heightMm,
        @Size(max = 1000) @Pattern(regexp = "^$|https://[^\\s]+|http://(localhost|127\\.0\\.0\\.1)(:[0-9]+)?/[^\\s]+", message = "must be an HTTPS URL (localhost allowed for development)") String modelUrl,
        @NotNull @Pattern(regexp = "#[0-9a-fA-F]{6}") String finishColor
) {}
