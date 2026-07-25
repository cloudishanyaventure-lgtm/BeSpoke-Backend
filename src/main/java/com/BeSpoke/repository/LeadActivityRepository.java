package com.BeSpoke.repository;

import com.BeSpoke.entity.ActivityType;
import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.LeadActivity;
import com.BeSpoke.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeadActivityRepository extends JpaRepository<LeadActivity, Long> {

    List<LeadActivity> findByLeadOrderByCreatedAtAsc(Lead lead);

    List<LeadActivity> findByLeadAndTypeOrderByCreatedAtAsc(Lead lead, ActivityType type);

    List<LeadActivity> findTop15ByOrderByCreatedAtDesc();

    List<LeadActivity> findTop15ByLead_AssignedDesignerOrderByCreatedAtDesc(User assignedDesigner);
}
