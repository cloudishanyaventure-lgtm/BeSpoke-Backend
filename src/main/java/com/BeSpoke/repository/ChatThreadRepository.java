package com.BeSpoke.repository;

import com.BeSpoke.entity.ChatThread;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatThreadRepository extends JpaRepository<ChatThread, Long> {

    List<ChatThread> findByCustomerIdOrDesignerIdOrderByCreatedAtDesc(Long customerId, Long designerId);

    Optional<ChatThread> findByLeadId(Long leadId);
}
