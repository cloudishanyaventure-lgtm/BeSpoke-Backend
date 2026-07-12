package com.BeSpoke.repository;

import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.LeadStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    List<Lead> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Lead> findByDesignerIdAndStatusOrderByCreatedAtDesc(Long designerId, LeadStatus status);

    List<Lead> findByDesignerIdAndStatusInOrderByCreatedAtDesc(Long designerId, Collection<LeadStatus> statuses);

    List<Lead> findByStatusOrderByCreatedAtDesc(LeadStatus status);

    List<Lead> findAllByOrderByCreatedAtDesc();

    long countByStatus(LeadStatus status);
}
