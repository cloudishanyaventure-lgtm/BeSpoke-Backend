package com.BeSpoke.service;

import com.BeSpoke.dto.InvoiceDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** openhtmltopdf + our template must actually produce a valid PDF, not throw at runtime. */
class PdfServiceTest {

    @Test
    void invoicePdfRendersValidPdf() {
        var seller = new InvoiceDto.Party("BeSpoke Studio", "29ABCDE1234F1Z5", "ABCDE1234F",
                "1 MG Road", "Bengaluru", "+91 98000 00000", "hi@bespoke.in");
        var buyer = new InvoiceDto.Party("Kapil Agarwal", null, null, null, "Kanpur",
                "+91 98370 00000", "kapil@example.com");
        var dto = new InvoiceDto(1L, 1L, "Kapil Agarwal — Independent house", "Kapil Agarwal",
                null, null, "INV-2026-000001", "Advance — 50% of the accepted quote",
                new BigDecimal("26904.00"), 18, new BigDecimal("31746.72"), BigDecimal.ZERO,
                new BigDecimal("31746.72"), LocalDate.now(), "SENT", Instant.now(),
                List.of(), seller, buyer);

        byte[] pdf = new PdfService().invoice(dto);

        assertTrue(pdf.length > 800, "PDF should have real content");
        assertEquals('%', (char) pdf[0]);
        assertEquals('P', (char) pdf[1]);
        assertEquals('D', (char) pdf[2]);
        assertEquals('F', (char) pdf[3]);
    }
}
