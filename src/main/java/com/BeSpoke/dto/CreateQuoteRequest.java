package com.BeSpoke.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateQuoteRequest(
        @NotNull Long leadId,
        @NotBlank @Size(max = 255) String title,
        /** One category for the whole proposal. */
        @NotBlank @jakarta.validation.constraints.Pattern(
                regexp = "DESIGN_DRAWINGS_STRUCTURE_INTERIOR|DESIGN_DRAWINGS_STRUCTURE"
                    + "|DESIGN_DRAWINGS_INTERIOR_MEP|DESIGN_DRAWINGS_INTERIOR_NO_MEP"
                    + "|DESIGN_WITH_PROJECT_MANAGEMENT|TURNKEY|MODULAR_FURNITURE") String category,
        LocalDate validUntil,
        @NotEmpty @Valid List<QuoteItemRequest> items
) {
}
