package com.BeSpoke.repository;
import com.BeSpoke.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface RoomPlanRepository extends JpaRepository<RoomPlan, Long> {
    List<RoomPlan> findByOwnerOrderByUpdatedAtDesc(User owner);
    List<RoomPlan> findByLeadOrderByUpdatedAtDesc(Lead lead);
    Optional<RoomPlan> findByIdAndOwner(Long id, User owner);
}
