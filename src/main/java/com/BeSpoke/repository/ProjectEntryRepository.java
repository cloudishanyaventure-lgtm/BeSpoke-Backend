package com.BeSpoke.repository;
import com.BeSpoke.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
public interface ProjectEntryRepository extends JpaRepository<ProjectEntry,Long>,JpaSpecificationExecutor<ProjectEntry> {}
