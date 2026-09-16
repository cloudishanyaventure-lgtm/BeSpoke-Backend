package com.BeSpoke.repository;

import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.Invoice;
import com.BeSpoke.entity.InvoiceStatus;
import com.BeSpoke.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    boolean existsByProjectAndScheduleCode(com.BeSpoke.entity.Project project, String scheduleCode);
    @org.springframework.data.jpa.repository.Query(value="select id from invoices where id=:id for update",nativeQuery=true)
    java.util.Optional<Long> lockRow(@org.springframework.data.repository.query.Param("id") Long id);

    List<Invoice> findByProjectOrderByCreatedAtAsc(Project project);

    List<Invoice> findByProjectInOrderByCreatedAtAsc(Collection<Project> projects);

    List<Invoice> findAllByOrderByCreatedAtDesc();

    List<Invoice> findByStatusOrderByCreatedAtDesc(InvoiceStatus status);

    Optional<Invoice> findFirstByOrderByIdDesc();

    List<Invoice> findByProject_Lead_CompanyOrderByCreatedAtDesc(Company company);
}
