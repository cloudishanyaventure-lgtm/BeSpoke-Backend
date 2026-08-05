package com.BeSpoke.dto;

/** One node of a company's org chart. reportsToUserId falls back to the role's default parent. */
public record OrgMemberDto(Long userId, String name, String role, String title,
                           boolean active, Long reportsToUserId) {
}
