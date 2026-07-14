package com.BeSpoke.repository;

import com.BeSpoke.entity.DesignerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface DesignerProfileRepository extends JpaRepository<DesignerProfile, Long> {

    Optional<DesignerProfile> findByUserId(Long userId);

    /** Atomic view-counter increment; returns the number of updated rows (0 = no such profile). */
    @Modifying(clearAutomatically = true)
    @Query("update DesignerProfile p set p.viewCount = p.viewCount + 1 where p.user.id = ?1")
    int incrementViewCountByUserId(Long userId);
}
