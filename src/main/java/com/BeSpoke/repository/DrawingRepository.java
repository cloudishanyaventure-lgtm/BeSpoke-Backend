package com.BeSpoke.repository;

import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.Drawing;
import com.BeSpoke.entity.DrawingStatus;
import com.BeSpoke.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DrawingRepository extends JpaRepository<Drawing, Long> {
    java.util.Optional<Drawing> findByDocumentId(Long id);

    List<Drawing> findByLeadOrderByCreatedAtDesc(Lead lead);

    List<Drawing> findByLead_CustomerAndStatusInOrderByCreatedAtDesc(
            com.BeSpoke.entity.User customer, java.util.Collection<DrawingStatus> statuses);

    /** Revision counter behind the derived title: "Kitchen — 1st Floor (v2)". */
    long countByLeadAndRequirementRoomId(Lead lead, Long requirementRoomId);

    /** Approvals queue, longest wait first. */
    List<Drawing> findByStatusAndLead_CompanyOrderBySubmittedAtAsc(DrawingStatus status, Company company);

    List<Drawing> findByStatusOrderBySubmittedAtAsc(DrawingStatus status);
}
