package com.BeSpoke.dto;

import java.util.List;
import java.util.Map;

public record DesignerDashboardDto(
        Map<String, Long> myLeadsByStage,
        List<LeadSummaryDto> followUpsDue,
        List<ProjectDto> myProjects,
        long unreadMessages,
        List<ActivityDto> recentActivities
) {
}
