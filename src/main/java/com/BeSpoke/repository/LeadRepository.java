package com.BeSpoke.repository;

import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.LeadStatus;
import com.BeSpoke.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LeadRepository extends JpaRepository<Lead, Long> {
    @org.springframework.data.jpa.repository.Query(value="select id from leads where id=:id for update",nativeQuery=true)
    Optional<Long> lockRow(@org.springframework.data.repository.query.Param("id")Long id);

    Optional<Lead> findFirstByCustomerOrderByCreatedAtDesc(User customer);

    List<Lead> findByCustomerOrderByCreatedAtDesc(User customer);

    /** Open lead for a contact address — a mail thread must not mint a lead per reply. */
    Optional<Lead> findFirstByContactEmailIgnoreCaseAndStatusNotInOrderByCreatedAtDesc(
            String contactEmail, Collection<LeadStatus> excluded);

    List<Lead> findAllByOrderByCreatedAtDesc();

    List<Lead> findByAssignedDesignerOrderByCreatedAtDesc(User assignedDesigner);

    List<Lead> findBySalesOwnerOrAssignedDesignerOrderByCreatedAtDesc(User salesOwner, User assignedDesigner);

    List<Lead> findByFollowUpAtLessThanEqualAndStatusNotInOrderByFollowUpAtAsc(
            LocalDate date, Collection<LeadStatus> excluded);

    List<Lead> findByFollowUpAtLessThanEqualAndStatusNotInAndAssignedDesignerOrderByFollowUpAtAsc(
            LocalDate date, Collection<LeadStatus> excluded, User assignedDesigner);

    long countByAssignedDesignerAndStatusNotIn(User assignedDesigner, Collection<LeadStatus> excluded);

    List<Lead> findByStatusNotIn(Collection<LeadStatus> excluded);

    List<Lead> findByCompanyOrderByCreatedAtDesc(Company company);

    long countByCompanyAndStatusNotIn(Company company, Collection<LeadStatus> excluded);

    long countByCompanyIsNullAndStatusNot(LeadStatus status);

    long countByStatusNotIn(Collection<LeadStatus> excluded);
}
