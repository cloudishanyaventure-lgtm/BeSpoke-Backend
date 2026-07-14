package com.BeSpoke.controller;

import com.BeSpoke.dto.CreateReviewRequest;
import com.BeSpoke.dto.ReviewDto;
import com.BeSpoke.entity.User;
import com.BeSpoke.service.CurrentUserService;
import com.BeSpoke.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final CurrentUserService currentUserService;

    public ReviewController(ReviewService reviewService, CurrentUserService currentUserService) {
        this.reviewService = reviewService;
        this.currentUserService = currentUserService;
    }

    /** CUSTOMER only (enforced in SecurityConfig). Upserts the customer's review of a designer. */
    @PostMapping
    public ResponseEntity<ReviewDto> create(Authentication authentication,
                                            @Valid @RequestBody CreateReviewRequest request) {
        User customer = currentUserService.requireByEmail(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createOrUpdate(customer, request));
    }
}
