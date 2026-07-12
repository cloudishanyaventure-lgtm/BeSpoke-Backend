package com.BeSpoke.controller;

import com.BeSpoke.dto.CheckoutRequest;
import com.BeSpoke.dto.CheckoutResponse;
import com.BeSpoke.entity.User;
import com.BeSpoke.service.CheckoutService;
import com.BeSpoke.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final CurrentUserService currentUserService;

    public CheckoutController(CheckoutService checkoutService, CurrentUserService currentUserService) {
        this.checkoutService = checkoutService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/api/checkout")
    public ResponseEntity<CheckoutResponse> checkout(Authentication authentication,
                                                     @Valid @RequestBody CheckoutRequest request) {
        User customer = currentUserService.requireByEmail(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(checkoutService.checkout(customer, request));
    }
}
