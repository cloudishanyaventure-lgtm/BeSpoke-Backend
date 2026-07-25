package com.BeSpoke.repository;

import com.BeSpoke.entity.Project;
import com.BeSpoke.entity.ProjectMilestone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectMilestoneRepository extends JpaRepository<ProjectMilestone, Long> {

    List<ProjectMilestone> findByProjectOrderBySortOrderAsc(Project project);

    void deleteByProject(Project project);
}
