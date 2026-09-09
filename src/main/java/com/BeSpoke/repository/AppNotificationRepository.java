package com.BeSpoke.repository;
import com.BeSpoke.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;
public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {
    List<AppNotification> findTop100ByRecipientOrderByCreatedAtDesc(User user);
    Optional<AppNotification> findByIdAndRecipient(Long id, User user);
    long countByRecipientAndReadAtIsNull(User user);
    @Modifying
    @Query("update AppNotification n set n.readAt = :at where n.recipient = :user and n.readAt is null")
    int markAllRead(@Param("user") User user, @Param("at") Instant at);
}
