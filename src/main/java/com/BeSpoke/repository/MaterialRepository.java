package com.BeSpoke.repository;

import com.BeSpoke.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Browsing goes through {@link JpaSpecificationExecutor}: the filter set is mostly
 * optional, and a criteria query simply omits the ones that aren't set — a single
 * JPQL statement full of "or :param is null" makes Postgres guess the type of an
 * unbound parameter, which it gets wrong (lower(bytea)).
 */
public interface MaterialRepository
        extends JpaRepository<Material, Long>, JpaSpecificationExecutor<Material> {

    Optional<Material> findBySlug(String slug);

    long countByCategoryIdAndActiveTrue(Long categoryId);

    List<Material> findByCategoryIdOrderBySortOrderAscNameAsc(Long categoryId);

    /** Same category, different item — the "similar products" rail on the detail pane. */
    @Query("select m from Material m where m.active = true and m.category.id = :categoryId"
            + " and m.id <> :id order by m.sortOrder")
    List<Material> findSimilar(@Param("categoryId") Long categoryId, @Param("id") Long id);
}
