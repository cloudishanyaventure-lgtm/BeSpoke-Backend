package com.BeSpoke.controller;

import com.BeSpoke.dto.CreateInvoiceRequest;
import com.BeSpoke.dto.InvoiceDto;
import com.BeSpoke.dto.RecordPaymentRequest;
import com.BeSpoke.entity.User;
import com.BeSpoke.service.CurrentUserService;
import com.BeSpoke.service.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Invoicing and payment ledger - platform admin and studio directors (company scoped). */
@RestController
@RequestMapping("/api/invoices")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DIRECTOR','ACCOUNT_MANAGER')")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final CurrentUserService currentUserService;

    public InvoiceController(InvoiceService invoiceService, CurrentUserService currentUserService) {
        this.invoiceService = invoiceService;
        this.currentUserService = currentUserService;
    }

    private User me(Authentication authentication) {
        return currentUserService.requireByEmail(authentication.getName());
    }

    @PostMapping
    public ResponseEntity<InvoiceDto> create(Authentication authentication,
                                             @Valid @RequestBody CreateInvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invoiceService.create(me(authentication), request));
    }

    @PostMapping("/{id}/send")
    public InvoiceDto send(Authentication authentication, @PathVariable Long id) {
        return invoiceService.send(me(authentication), id);
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<InvoiceDto> recordPayment(Authentication authentication,
                                                    @PathVariable Long id,
                                                    @Valid @RequestBody RecordPaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invoiceService.recordPayment(me(authentication), id, request));
    }

    @GetMapping
    public List<InvoiceDto> list(Authentication authentication,
                                 @RequestParam(required = false) String status) {
        return invoiceService.list(me(authentication), status);
    }
}
