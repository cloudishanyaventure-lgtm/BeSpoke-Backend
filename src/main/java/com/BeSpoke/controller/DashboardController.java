package com.BeSpoke.controller;

import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.User;
import com.BeSpoke.service.CurrentUserService;
import com.BeSpoke.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Role-shaped dashboard: admins get the command center, designers their own queue. */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentUserService currentUserService;

    public DashboardController(DashboardService dashboardService, CurrentUserService currentUserService) {
        this.dashboardService = dashboardService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public Object dashboard(Authentication authentication) {
        User current = currentUserService.requireByEmail(authentication.getName());
        return current.getRole() == Role.ADMIN
                ? dashboardService.adminDashboard()
                : dashboardService.designerDashboard(current);
    }
}
