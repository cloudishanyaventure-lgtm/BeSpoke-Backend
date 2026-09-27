package com.BeSpoke.dto;

import com.BeSpoke.entity.ProjectMember;
import com.BeSpoke.entity.User;

import java.time.Instant;

/** One row in the project's people list: the owner, or an invited family member. */
public record ProjectMemberDto(
        Long id,            // membership id; null for the owner row
        String name,
        String email,
        String phone,
        String status,      // OWNER | INVITED | JOINED
        boolean owner,
        boolean isMe,
        String familyMember,
        Instant joinedAt
) {

    public static ProjectMemberDto owner(User owner, User caller) {
        boolean me = owner != null && caller != null && owner.getId().equals(caller.getId());
        return new ProjectMemberDto(null,
                owner == null ? "Project owner" : owner.getName(),
                owner == null ? null : owner.getEmail(),
                owner == null ? null : owner.getPhone(),
                "OWNER", true, me, null, null);
    }

    public static ProjectMemberDto from(ProjectMember m, User caller) {
        boolean me = m.getUser() != null && caller != null && m.getUser().getId().equals(caller.getId());
        String name = m.getUser() != null ? m.getUser().getName()
                : (m.getInvitedName() != null && !m.getInvitedName().isBlank()
                    ? m.getInvitedName() : m.getInvitedEmail());
        String phone = m.getUser() != null ? m.getUser().getPhone() : m.getInvitedPhone();
        return new ProjectMemberDto(m.getId(), name, m.getInvitedEmail(), phone,
                m.getStatus().name(), false, me, m.getFamilyMember(), m.getJoinedAt());
    }
}
