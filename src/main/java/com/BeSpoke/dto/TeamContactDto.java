package com.BeSpoke.dto;

/**
 * One colleague in the team-chat picker. Same shape as OrgMemberDto (so the frontend can
 * reuse the org-tree layout) plus the unread count of their messages to me.
 */
public record TeamContactDto(Long userId, String name, String role, String title,
                             Long reportsToUserId, boolean active, long unread) {
}
