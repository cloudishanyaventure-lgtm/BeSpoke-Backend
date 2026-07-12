package com.BeSpoke.repository;

import com.BeSpoke.entity.DesignService;
import com.BeSpoke.entity.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DesignServiceRepository extends JpaRepository<DesignService, Long> {

    List<DesignService> findByCategory(ServiceCategory category);
}
