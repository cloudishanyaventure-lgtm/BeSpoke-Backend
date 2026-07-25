package com.BeSpoke.controller;

import com.BeSpoke.dto.MilestoneRequest;
import com.BeSpoke.dto.ProjectDetailDto;
import com.BeSpoke.dto.ProjectDto;
import com.BeSpoke.dto.UpdateProjectRequest;
import com.BeSpoke.entity.User;
import com.BeSpoke.service.CurrentUserService;
import com.BeSpoke.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Staff projects. Designers see only their own; financials are admin-only. */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final CurrentUserService currentUserService;

    public ProjectController(ProjectService projectService, CurrentUserService currentUserService) {
        this.projectService = projectService;
        this.currentUserService = currentUserService;
    }

    private User me(Authentication authentication) {
        return currentUserService.requireByEmail(authentication.getName());
    }

    @GetMapping
    public List<ProjectDto> list(Authentication authentication) {
        return projectService.list(me(authentication));
    }

    @GetMapping("/{id}")
    public ProjectDetailDto get(Authentication authentication, @PathVariable Long id) {
        return projectService.get(me(authentication), id);
    }

    @PutMapping("/{id}")
    public ProjectDetailDto update(Authentication authentication,
                                   @PathVariable Long id,
                                   @Valid @RequestBody UpdateProjectRequest request) {
        return projectService.update(me(authentication), id, request);
    }

    @PutMapping("/{id}/milestones")
    public ProjectDetailDto updateMilestones(Authentication authentication,
                                             @PathVariable Long id,
                                             @Valid @RequestBody List<MilestoneRequest> requests) {
        return projectService.updateMilestones(me(authentication), id, requests);
    }
}
