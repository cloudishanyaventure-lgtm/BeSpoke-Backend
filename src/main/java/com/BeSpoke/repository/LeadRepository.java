package com.BeSpoke.repository;

import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.LeadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    List<Lead> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Lead> findByDesignerIdAndStatusOrderByCreatedAtDesc(Long designerId, LeadStatus status);

    List<Lead> findByDesignerIdAndStatusInOrderByCreatedAtDesc(Long designerId, Collection<LeadStatus> statuses);

    List<Lead> findByStatusOrderByCreatedAtDesc(LeadStatus status);

    List<Lead> findAllByOrderByCreatedAtDesc();

    long countByStatus(LeadStatus status);

    long countByDesignerIdAndStatus(Long designerId, LeadStatus status);

    long countByDesignerIdAndStatusIn(Long designerId, Collection<LeadStatus> statuses);

    /**
     * Sum of order totals for a designer's leads in the given statuses (order-backed leads only).
     * Returns null when there are no matching leads.
     */
    @Query("select sum(l.order.totalAmount) from Lead l "
            + "where l.designer.id = ?1 and l.status in ?2")
    BigDecimal sumOrderTotalsForDesigner(Long designerId, Collection<LeadStatus> statuses);
}
