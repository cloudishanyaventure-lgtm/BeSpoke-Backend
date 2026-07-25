package com.BeSpoke.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateInvoiceRequest(
        @NotNull Long projectId,
        Long milestoneId,
        @NotBlank @Size(max = 255) String title,
        @NotNull @Positive BigDecimal amount,
        @Min(0) @Max(28) int gstPct,
        LocalDate dueDate
) {
}
