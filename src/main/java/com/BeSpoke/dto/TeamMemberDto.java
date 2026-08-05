package com.BeSpoke.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/** openLeads / activeProjects are included for admins only. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TeamMemberDto(
        Long userId,
        String name,
        String email,
        String phone,
        String role,
        String city,
        String title,
        String dept,
        boolean active,
        Long openLeads,
        Long activeProjects,
        String company
) {
}
