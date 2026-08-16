package com.BeSpoke.repository;

import com.BeSpoke.entity.MaterialBrand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MaterialBrandRepository extends JpaRepository<MaterialBrand, Long> {

    Optional<MaterialBrand> findBySlug(String slug);

    List<MaterialBrand> findAllByOrderByNameAsc();

    @Query("select b from MaterialBrand b join b.categories c"
            + " where c.slug = :slug and b.active = true order by b.name")
    List<MaterialBrand> findByCategorySlug(@Param("slug") String slug);
}
