package com.BeSpoke.dto;

import com.BeSpoke.entity.Review;

import java.time.Instant;

public record ReviewDto(
        Long id,
        int rating,
        String comment,
        String customerName,
        Instant createdAt
) {

    public static ReviewDto from(Review review) {
        return new ReviewDto(
                review.getId(),
                review.getRating(),
                review.getComment(),
                review.getCustomer().getName(),
                review.getCreatedAt()
        );
    }
}
