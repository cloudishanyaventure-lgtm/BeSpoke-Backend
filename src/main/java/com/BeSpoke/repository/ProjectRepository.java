package com.BeSpoke.repository;

import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.Project;
import com.BeSpoke.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByLead(Lead lead);

    List<Project> findAllByOrderByCreatedAtDesc();

    List<Project> findByDesignerOrderByCreatedAtDesc(User designer);

    List<Project> findByClientOrderByCreatedAtDesc(User client);

    /** Active = work has actually started: a payment recorded or a drawing on the lead. */
    @Query("""
            select count(p) from Project p
            where p.designer = :designer
              and (exists (select 1 from InvoicePayment ip where ip.invoice.project = p)
                or exists (select 1 from Drawing d where d.lead = p.lead))
            """)
    long countActiveByDesigner(@Param("designer") User designer);

    List<Project> findByLead_CompanyOrderByCreatedAtDesc(Company company);
}
