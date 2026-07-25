package com.BeSpoke.repository;

import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.Project;
import com.BeSpoke.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByLead(Lead lead);

    List<Project> findAllByOrderByCreatedAtDesc();

    List<Project> findByDesignerOrderByCreatedAtDesc(User designer);

    List<Project> findByClientOrderByCreatedAtDesc(User client);

    long countByDesigner(User designer);
}
