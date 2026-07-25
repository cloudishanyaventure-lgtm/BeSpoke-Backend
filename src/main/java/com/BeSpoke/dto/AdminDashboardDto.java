package com.BeSpoke.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AdminDashboardDto(
        BigDecimal revenueCollected,
        BigDecimal outstanding,
        BigDecimal pipelineValue,
        Map<String, Long> leadsByStage,
        List<LeadSummaryDto> followUpsDue,
        Map<String, Long> projectHealth,
        List<ActivityDto> recentActivities,
        List<TeamLoadDto> teamLoad
) {

    public record TeamLoadDto(Long userId, String name, String title, long openLeads, long activeProjects) {
    }
}
