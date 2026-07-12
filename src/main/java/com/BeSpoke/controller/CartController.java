package com.BeSpoke.controller;

import com.BeSpoke.dto.CartItemDto;
import com.BeSpoke.dto.CartItemRequest;
import com.BeSpoke.entity.User;
import com.BeSpoke.service.CartService;
import com.BeSpoke.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;
    private final CurrentUserService currentUserService;

    public CartController(CartService cartService, CurrentUserService currentUserService) {
        this.cartService = cartService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<CartItemDto> getCart(Authentication authentication) {
        User user = currentUserService.requireByEmail(authentication.getName());
        return cartService.getCart(user);
    }

    @PostMapping
    public ResponseEntity<CartItemDto> addToCart(Authentication authentication,
                                                 @Valid @RequestBody CartItemRequest request) {
        User user = currentUserService.requireByEmail(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(cartService.addToCart(user, request));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> removeItem(Authentication authentication, @PathVariable Long itemId) {
        User user = currentUserService.requireByEmail(authentication.getName());
        cartService.removeFromCart(user, itemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        User user = currentUserService.requireByEmail(authentication.getName());
        cartService.clearCart(user);
        return ResponseEntity.noContent().build();
    }
}
