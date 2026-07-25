package com.BeSpoke.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record QuoteItemRequest(
        @NotBlank @Pattern(regexp = "DESIGN_FEES|MATERIALS|LABOUR|PROJECT_MANAGEMENT",
                message = "category must be DESIGN_FEES, MATERIALS, LABOUR or PROJECT_MANAGEMENT") String category,
        @NotBlank @Size(max = 500) String description,
        @NotNull @Positive BigDecimal qty,
        @NotNull @Positive BigDecimal rate,
        @Min(0) @Max(28) int gstPct
) {
}
