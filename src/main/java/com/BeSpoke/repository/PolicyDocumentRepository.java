package com.BeSpoke.repository;

import com.BeSpoke.entity.PolicyDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PolicyDocumentRepository extends JpaRepository<PolicyDocument, Long> {

    List<PolicyDocument> findByActiveTrueOrderBySortOrderAsc();

    List<PolicyDocument> findAllByOrderBySortOrderAsc();

    Optional<PolicyDocument> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
