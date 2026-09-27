package com.BeSpoke.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record QuoteItemRequest(
        @NotBlank @Size(max = 500) String description,
        // 0 is valid: a room heading (turnkey) and a scope-only element line both carry no
        // quantity/rate — they organise the proposal, the priced lines sit beside them.
        @NotNull @PositiveOrZero BigDecimal qty,
        @NotNull @PositiveOrZero BigDecimal rate,
        @Min(0) @Max(28) int gstPct,
        @Size(max = 200) String section,
        boolean heading
) {
}
