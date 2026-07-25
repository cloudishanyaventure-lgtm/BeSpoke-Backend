package com.BeSpoke.repository;

import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.LeadStatus;
import com.BeSpoke.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    Optional<Lead> findFirstByCustomerOrderByCreatedAtDesc(User customer);

    List<Lead> findByCustomerOrderByCreatedAtDesc(User customer);

    List<Lead> findAllByOrderByCreatedAtDesc();

    List<Lead> findByAssignedDesignerOrderByCreatedAtDesc(User assignedDesigner);

    List<Lead> findByFollowUpAtLessThanEqualAndStatusNotInOrderByFollowUpAtAsc(
            LocalDate date, Collection<LeadStatus> excluded);

    List<Lead> findByFollowUpAtLessThanEqualAndStatusNotInAndAssignedDesignerOrderByFollowUpAtAsc(
            LocalDate date, Collection<LeadStatus> excluded, User assignedDesigner);

    long countByAssignedDesignerAndStatusNotIn(User assignedDesigner, Collection<LeadStatus> excluded);

    List<Lead> findByStatusNotIn(Collection<LeadStatus> excluded);
}
