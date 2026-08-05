package com.BeSpoke.repository;

import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.Drawing;
import com.BeSpoke.entity.DrawingStatus;
import com.BeSpoke.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DrawingRepository extends JpaRepository<Drawing, Long> {

    List<Drawing> findByLeadOrderByCreatedAtDesc(Lead lead);

    /** Revision counter behind the derived title: "Kitchen — 1st Floor (v2)". */
    long countByLeadAndRequirementRoomId(Lead lead, Long requirementRoomId);

    /** Approvals queue, longest wait first. */
    List<Drawing> findByStatusAndLead_CompanyOrderBySubmittedAtAsc(DrawingStatus status, Company company);

    List<Drawing> findByStatusOrderBySubmittedAtAsc(DrawingStatus status);
}
