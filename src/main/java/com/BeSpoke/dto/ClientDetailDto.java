package com.BeSpoke.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/** quotes and invoices are admin-only and omitted for designers. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClientDetailDto(
        ClientDto client,
        List<LeadSummaryDto> leads,
        List<ProjectDto> projects,
        List<QuoteDto> quotes,
        List<InvoiceDto> invoices
) {
}
