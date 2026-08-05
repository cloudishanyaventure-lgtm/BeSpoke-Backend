package com.BeSpoke.controller;

import com.BeSpoke.dto.PlaceOrderRequest;
import com.BeSpoke.dto.ShopOrderDto;
import com.BeSpoke.entity.User;
import com.BeSpoke.service.CurrentUserService;
import com.BeSpoke.service.ShopService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Customer checkout + order history. URL security scopes both paths to
 * CUSTOMER (POST /api/orders and /api/my/**).
 */
@RestController
@RequestMapping("/api")
public class OrderController {

    private final ShopService shopService;
    private final CurrentUserService currentUserService;

    public OrderController(ShopService shopService, CurrentUserService currentUserService) {
        this.shopService = shopService;
        this.currentUserService = currentUserService;
    }

    private User me(Authentication authentication) {
        return currentUserService.requireByEmail(authentication.getName());
    }

    /** Splits the cart into one order per vendor; returns every order placed. */
    @PostMapping("/orders")
    public ResponseEntity<List<ShopOrderDto>> place(Authentication authentication,
                                                    @Valid @RequestBody PlaceOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(shopService.place(me(authentication), request));
    }

    @GetMapping("/my/orders")
    public List<ShopOrderDto> myOrders(Authentication authentication) {
        return shopService.myOrders(me(authentication));
    }
}
