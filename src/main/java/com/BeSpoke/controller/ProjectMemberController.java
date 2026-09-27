package com.BeSpoke.controller;

import com.BeSpoke.dto.AuthResponse;
import com.BeSpoke.dto.ProjectContextDto;
import com.BeSpoke.service.CurrentUserService;
import com.BeSpoke.service.ProjectMemberService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Family members on a customer's project. The management endpoints live under
 * {@code /api/my} (customer-only, per SecurityConfig); accepting an invite is public
 * because the invitee has no session yet.
 */
@RestController
public class ProjectMemberController {

    private final ProjectMemberService service;
    private final CurrentUserService currentUserService;

    public ProjectMemberController(ProjectMemberService service, CurrentUserService currentUserService) {
        this.service = service;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/api/my/members")
    public ProjectContextDto members(Authentication auth) {
        return service.context(me(auth));
    }

    @PostMapping("/api/my/members")
    public ProjectContextDto invite(Authentication auth, @RequestBody InviteRequest request) {
        return service.invite(me(auth), request.name(), request.email(), request.phone());
    }

    @DeleteMapping("/api/my/members/{id}")
    public ProjectContextDto remove(Authentication auth, @PathVariable Long id) {
        return service.remove(me(auth), id);
    }

    @PostMapping("/api/my/members/claim-room")
    public ProjectContextDto claimRoom(Authentication auth, @RequestBody ClaimRoomRequest request) {
        return service.claimRoom(me(auth), request.familyMember());
    }

    /** Public: create/bind the account and sign in. */
    @PostMapping("/api/invites/{token}/accept")
    public AuthResponse accept(@PathVariable String token) {
        return service.accept(token);
    }

    private com.BeSpoke.entity.User me(Authentication auth) {
        return currentUserService.requireByEmail(auth.getName());
    }

    public record InviteRequest(String name, @NotBlank String email, String phone) {
    }

    public record ClaimRoomRequest(String familyMember) {
    }
}
