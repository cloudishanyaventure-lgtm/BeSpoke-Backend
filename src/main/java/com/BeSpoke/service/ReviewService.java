package com.BeSpoke.service;

import com.BeSpoke.dto.CreateReviewRequest;
import com.BeSpoke.dto.ReviewDto;
import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.Review;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.DesignerProfileRepository;
import com.BeSpoke.repository.LeadRepository;
import com.BeSpoke.repository.ReviewRepository;
import com.BeSpoke.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final LeadRepository leadRepository;
    private final DesignerProfileRepository designerProfileRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         UserRepository userRepository,
                         LeadRepository leadRepository,
                         DesignerProfileRepository designerProfileRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.leadRepository = leadRepository;
        this.designerProfileRepository = designerProfileRepository;
    }

    /** Creates a review, or updates the customer's existing review of the same designer. */
    @Transactional
    public ReviewDto createOrUpdate(User customer, CreateReviewRequest request) {
        User designer = userRepository.findById(request.designerUserId())
                .orElseThrow(() -> new NotFoundException("Designer not found: " + request.designerUserId()));
        if (designer.getRole() != Role.DESIGNER || !designer.isActive()) {
            throw new BadRequestException("Reviews can only be left for active designers");
        }
        Lead lead = null;
        if (request.leadId() != null) {
            lead = leadRepository.findById(request.leadId())
                    .orElseThrow(() -> new NotFoundException("Lead not found: " + request.leadId()));
        }

        Review review = reviewRepository.findByDesignerIdAndCustomerId(designer.getId(), customer.getId())
                .orElse(null);
        if (review == null) {
            review = new Review(designer, customer, lead, request.rating(), request.comment());
        } else {
            review.setRating(request.rating());
            review.setComment(request.comment());
            if (lead != null) {
                review.setLead(lead);
            }
            review.setCreatedAt(Instant.now());
        }
        review = reviewRepository.saveAndFlush(review);

        // Keep the stored profile rating in sync with the live review average.
        Double average = reviewRepository.averageRatingForDesigner(designer.getId());
        designerProfileRepository.findByUserId(designer.getId()).ifPresent(profile -> {
            profile.setRating(average);
            designerProfileRepository.save(profile);
        });
        return ReviewDto.from(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> reviewsForDesigner(Long designerUserId) {
        User designer = userRepository.findById(designerUserId)
                .orElseThrow(() -> new NotFoundException("Designer not found: " + designerUserId));
        if (designer.getRole() != Role.DESIGNER) {
            throw new NotFoundException("Designer not found: " + designerUserId);
        }
        return reviewRepository.findByDesignerIdOrderByCreatedAtDesc(designerUserId)
                .stream().map(ReviewDto::from).toList();
    }
}
