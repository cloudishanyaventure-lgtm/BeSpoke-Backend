package com.BeSpoke.repository;

import com.BeSpoke.entity.PlatformOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlatformOptionRepository extends JpaRepository<PlatformOption, Long> {

    List<PlatformOption> findByActiveTrueOrderByListKeyAscSortOrderAsc();

    List<PlatformOption> findAllByOrderByListKeyAscSortOrderAsc();

    List<PlatformOption> findByListKeyOrderBySortOrderAsc(String listKey);

    Optional<PlatformOption> findByListKeyAndValue(String listKey, String value);

    boolean existsByListKey(String listKey);
}
