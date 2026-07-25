package com.BeSpoke.repository;

import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.RequirementForm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RequirementFormRepository extends JpaRepository<RequirementForm, Long> {

    Optional<RequirementForm> findByLead(Lead lead);
}
