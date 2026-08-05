package com.BeSpoke.repository;

import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.TeamMessage;
import com.BeSpoke.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamMessageRepository extends JpaRepository<TeamMessage, Long> {

    /**
     * Both directions of one thread: pass the same two-user list for senders and recipients.
     * Self-messages would also match, which is why TeamChatService refuses them.
     */
    List<TeamMessage> findByCompanyAndSenderInAndRecipientInOrderByCreatedAtAsc(
            Company company, List<User> senders, List<User> recipients);

    long countByCompanyAndRecipientAndSenderAndReadAtIsNull(Company company, User recipient, User sender);

    List<TeamMessage> findByCompanyAndRecipientAndSenderAndReadAtIsNull(
            Company company, User recipient, User sender);
}
