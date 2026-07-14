package com.BeSpoke.controller;

import com.BeSpoke.dto.AdminOverviewDto;
import com.BeSpoke.dto.ChatMessageDto;
import com.BeSpoke.dto.ChatThreadDto;
import com.BeSpoke.dto.LeadDto;
import com.BeSpoke.entity.User;
import com.BeSpoke.service.ChatService;
import com.BeSpoke.service.CurrentUserService;
import com.BeSpoke.service.LeadService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final LeadService leadService;
    private final ChatService chatService;
    private final CurrentUserService currentUserService;

    public AdminController(LeadService leadService,
                           ChatService chatService,
                           CurrentUserService currentUserService) {
        this.leadService = leadService;
        this.chatService = chatService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/leads")
    public List<LeadDto> leads(@RequestParam(required = false) String status) {
        return leadService.adminLeads(status);
    }

    @PostMapping("/leads/{id}/assign/{designerId}")
    public LeadDto assign(@PathVariable Long id, @PathVariable Long designerId) {
        return leadService.assignDesigner(id, designerId);
    }

    @GetMapping("/overview")
    public AdminOverviewDto overview() {
        return leadService.overview();
    }

    /** Read-only oversight: every chat thread on the platform. */
    @GetMapping("/threads")
    public List<ChatThreadDto> threads() {
        return chatService.allThreads();
    }

    /** Read-only oversight: messages of any thread. */
    @GetMapping("/threads/{id}/messages")
    public List<ChatMessageDto> threadMessages(Authentication authentication, @PathVariable Long id) {
        User admin = currentUserService.requireByEmail(authentication.getName());
        return chatService.messages(admin, id);
    }
}
