package com.BeSpoke.repository;

import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.ProjectMember;
import com.BeSpoke.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    Optional<ProjectMember> findByInviteToken(String inviteToken);

    List<ProjectMember> findByLeadOrderByCreatedAtAsc(Lead lead);

    Optional<ProjectMember> findByLeadAndUser(Lead lead, User user);

    boolean existsByLeadAndUser(Lead lead, User user);

    boolean existsByLeadAndInvitedEmailIgnoreCase(Lead lead, String invitedEmail);

    /** A joined membership for this user, used to resolve the project they were invited into. */
    Optional<ProjectMember> findFirstByUserAndStatusOrderByJoinedAtDesc(User user, ProjectMember.Status status);

    /** Every project this user has joined, for the customer's project switcher. */
    List<ProjectMember> findByUserAndStatus(User user, ProjectMember.Status status);
}
