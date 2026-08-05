package com.BeSpoke.controller;

import com.BeSpoke.dto.SendMessageRequest;
import com.BeSpoke.dto.TeamContactDto;
import com.BeSpoke.dto.TeamMessageDto;
import com.BeSpoke.service.CurrentUserService;
import com.BeSpoke.service.TeamChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Internal team chat. The URL rule allows every staff role; TeamChatService then
 * requires a company, so platform accounts and customers are refused.
 */
@RestController
@RequestMapping("/api/team-chat")
public class TeamChatController {

    private final TeamChatService teamChatService;
    private final CurrentUserService currentUserService;

    public TeamChatController(TeamChatService teamChatService, CurrentUserService currentUserService) {
        this.teamChatService = teamChatService;
        this.currentUserService = currentUserService;
    }

    /** The hierarchy picker: colleagues, their reporting line, and unread counts. */
    @GetMapping("/contacts")
    public List<TeamContactDto> contacts(Authentication authentication) {
        return teamChatService.contacts(currentUserService.requireByEmail(authentication.getName()));
    }

    @GetMapping("/{userId}")
    public List<TeamMessageDto> thread(Authentication authentication, @PathVariable Long userId) {
        return teamChatService.thread(
                currentUserService.requireByEmail(authentication.getName()), userId);
    }

    @PostMapping("/{userId}")
    public ResponseEntity<TeamMessageDto> send(Authentication authentication,
                                              @PathVariable Long userId,
                                              @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamChatService.send(
                currentUserService.requireByEmail(authentication.getName()), userId, request.body()));
    }
}
