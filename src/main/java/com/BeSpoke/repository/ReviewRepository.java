package com.BeSpoke.repository;

import com.BeSpoke.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByDesignerIdOrderByCreatedAtDesc(Long designerUserId);

    Optional<Review> findByDesignerIdAndCustomerId(Long designerUserId, Long customerId);

    long countByDesignerId(Long designerUserId);

    /** Live average rating for a designer, or null when the designer has no reviews. */
    @Query("select avg(r.rating) from Review r where r.designer.id = ?1")
    Double averageRatingForDesigner(Long designerUserId);
}
