package com.BeSpoke.controller;

import com.BeSpoke.dto.CreateQuoteRequest;
import com.BeSpoke.dto.QuoteDto;
import com.BeSpoke.dto.UpdateQuoteRequest;
import com.BeSpoke.entity.User;
import com.BeSpoke.service.CurrentUserService;
import com.BeSpoke.service.QuoteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Quote builder - platform admin and studio funnel roles; scoped to the studio in the service. */
@RestController
@RequestMapping("/api/quotes")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DIRECTOR','ACCOUNT_MANAGER','DESIGN_MANAGER','SALES_MANAGER','CUSTOMER_CONSULTANT')")
public class QuoteController {

    private final QuoteService quoteService;
    private final CurrentUserService currentUserService;

    public QuoteController(QuoteService quoteService, CurrentUserService currentUserService) {
        this.quoteService = quoteService;
        this.currentUserService = currentUserService;
    }

    private User me(Authentication authentication) {
        return currentUserService.requireByEmail(authentication.getName());
    }

    @PostMapping
    public ResponseEntity<QuoteDto> create(Authentication authentication,
                                           @Valid @RequestBody CreateQuoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(quoteService.create(me(authentication), request));
    }

    @PutMapping("/{id}")
    public QuoteDto update(Authentication authentication,
                           @PathVariable Long id,
                           @Valid @RequestBody UpdateQuoteRequest request) {
        return quoteService.update(me(authentication), id, request);
    }

    @PostMapping("/{id}/send")
    public QuoteDto send(Authentication authentication, @PathVariable Long id) {
        return quoteService.send(me(authentication), id);
    }

    @PostMapping("/{id}/revise")
    public ResponseEntity<QuoteDto> revise(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(quoteService.revise(me(authentication), id));
    }

    @GetMapping
    public List<QuoteDto> list(Authentication authentication,
                               @RequestParam(required = false) String status) {
        return quoteService.list(me(authentication), status);
    }

    @GetMapping("/{id}")
    public QuoteDto get(Authentication authentication, @PathVariable Long id) {
        return quoteService.get(me(authentication), id);
    }
}
