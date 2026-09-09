package com.BeSpoke.dto;
import jakarta.validation.constraints.*;

/** Numeric swatches refer to the version-one Room Lab palette. */
public record RoomPlanRequest(
    @NotBlank @Size(max = 60) String name,
    @NotNull @Min(0) @Max(3) Integer wall,
    @NotNull @Min(0) @Max(2) Integer floor,
    @NotNull @Min(0) @Max(2) Integer fabric,
    @NotNull @DecimalMin("3.6") @DecimalMax("6.0") Double width,
    @NotNull @DecimalMin("3.6") @DecimalMax("6.0") Double depth,
    @NotBlank @Pattern(regexp = "conversation|open") String layout,
    @NotNull Boolean rug,
    @Min(0) Long version
) {}
