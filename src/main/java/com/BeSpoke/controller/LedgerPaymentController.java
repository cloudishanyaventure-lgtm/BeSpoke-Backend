package com.BeSpoke.controller;

import com.BeSpoke.dto.LedgerPaymentDto;
import com.BeSpoke.dto.LedgerPaymentRequest;
import com.BeSpoke.service.CurrentUserService;
import com.BeSpoke.service.LedgerPaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Payments received from customers and payments made to vendors. Finance roles only. */
@RestController
@RequestMapping("/api/payments")
public class LedgerPaymentController {

    private final LedgerPaymentService paymentService;
    private final CurrentUserService currentUserService;

    public LedgerPaymentController(LedgerPaymentService paymentService,
                                   CurrentUserService currentUserService) {
        this.paymentService = paymentService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<LedgerPaymentDto> list(Authentication auth,
                                       @RequestParam(defaultValue = "RECEIVED") String direction) {
        return paymentService.list(currentUserService.requireByEmail(auth.getName()), direction);
    }

    @PostMapping
    public ResponseEntity<LedgerPaymentDto> record(Authentication auth,
                                                   @Valid @RequestBody LedgerPaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.record(
                currentUserService.requireByEmail(auth.getName()), request));
    }
}
