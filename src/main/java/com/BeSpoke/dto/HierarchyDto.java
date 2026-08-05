package com.BeSpoke.dto;

import java.util.List;

/** One call powers the whole platform org-chart dashboard. */
public record HierarchyDto(PlatformDto platform, List<CompanyNodeDto> companies) {

    public record PlatformDto(List<PlatformAdminDto> admins) {
    }

    public record PlatformAdminDto(Long userId, String name, String role, boolean active) {
    }

    public record CompanyNodeDto(Long id, String name, String type, Boolean solo, String kycStatus,
                                 String city, boolean active, long memberCount, long openLeads,
                                 long orderCount, String directorName, List<OrgMemberDto> members) {
    }
}
