package com.BeSpoke.repository;

import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.Message;
import com.BeSpoke.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByLeadOrderByCreatedAtAsc(Lead lead);

    Optional<Message> findFirstByLeadOrderByCreatedAtDesc(Lead lead);

    boolean existsByLead(Lead lead);

    long countByLeadAndSenderNotAndReadAtIsNull(Lead lead, User sender);

    long countByLead_AssignedDesignerAndSenderNotAndReadAtIsNull(User assignedDesigner, User sender);

    List<Message> findByLeadAndSenderNotAndReadAtIsNull(Lead lead, User sender);
}
