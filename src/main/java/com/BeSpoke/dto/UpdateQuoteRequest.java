package com.BeSpoke.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** DRAFT quotes only. Items replace the existing lines wholesale. */
public record UpdateQuoteRequest(
        @NotBlank @Size(max = 255) String title,
        LocalDate validUntil,
        @NotEmpty @Valid List<QuoteItemRequest> items
) {
}
