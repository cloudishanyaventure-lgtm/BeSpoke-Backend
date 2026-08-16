package com.BeSpoke.repository;

import com.BeSpoke.entity.MaterialCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaterialCategoryRepository extends JpaRepository<MaterialCategory, Long> {

    Optional<MaterialCategory> findBySlug(String slug);

    List<MaterialCategory> findAllByOrderBySortOrderAscNameAsc();

    List<MaterialCategory> findByActiveTrueOrderBySortOrderAscNameAsc();
}
