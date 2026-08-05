package com.BeSpoke.repository;

import com.BeSpoke.entity.AuditEvent;
import com.BeSpoke.entity.Company;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findByCompanyOrderByCreatedAtDesc(Company company, Pageable pageable);

    List<AuditEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
