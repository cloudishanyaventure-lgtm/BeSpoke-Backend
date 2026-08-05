package com.BeSpoke.controller;

import com.BeSpoke.dto.CreateTeamMemberRequest;
import com.BeSpoke.dto.TeamMemberDto;
import com.BeSpoke.dto.UpdateTeamMemberRequest;
import com.BeSpoke.entity.User;
import com.BeSpoke.service.CurrentUserService;
import com.BeSpoke.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Team directory (staff-readable, company scoped) and management (admin / director). */
@RestController
@RequestMapping("/api/team")
public class TeamController {

    private final TeamService teamService;
    private final CurrentUserService currentUserService;

    public TeamController(TeamService teamService, CurrentUserService currentUserService) {
        this.teamService = teamService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<TeamMemberDto> list(Authentication authentication) {
        User current = currentUserService.requireByEmail(authentication.getName());
        return teamService.list(current);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DIRECTOR')")
    public ResponseEntity<TeamMemberDto> create(Authentication authentication,
                                                @Valid @RequestBody CreateTeamMemberRequest request) {
        User current = currentUserService.requireByEmail(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.create(current, request));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DIRECTOR')")
    public TeamMemberDto update(Authentication authentication,
                                @PathVariable Long userId,
                                @Valid @RequestBody UpdateTeamMemberRequest request) {
        User current = currentUserService.requireByEmail(authentication.getName());
        return teamService.update(current, userId, request);
    }
}
