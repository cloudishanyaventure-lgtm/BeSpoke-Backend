package com.BeSpoke.repository;

import com.BeSpoke.entity.DrawingSchedule;
import com.BeSpoke.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DrawingScheduleRepository extends JpaRepository<DrawingSchedule, Long> {
    Optional<DrawingSchedule> findByLeadAndKind(Lead lead, DrawingSchedule.Kind kind);
}
