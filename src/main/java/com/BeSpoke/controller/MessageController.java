package com.BeSpoke.controller;

import com.BeSpoke.dto.MessageDto;
import com.BeSpoke.dto.SendMessageRequest;
import com.BeSpoke.dto.ThreadDto;
import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.User;
import com.BeSpoke.service.CurrentUserService;
import com.BeSpoke.service.LeadService;
import com.BeSpoke.service.MessageService;
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

/** Staff messaging. A thread == a lead; designers reach only their assigned leads. */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;
    private final LeadService leadService;
    private final CurrentUserService currentUserService;

    public MessageController(MessageService messageService,
                             LeadService leadService,
                             CurrentUserService currentUserService) {
        this.messageService = messageService;
        this.leadService = leadService;
        this.currentUserService = currentUserService;
    }

    private User me(Authentication authentication) {
        return currentUserService.requireByEmail(authentication.getName());
    }

    @GetMapping("/threads")
    public List<ThreadDto> threads(Authentication authentication) {
        return messageService.threads(me(authentication));
    }

    @GetMapping("/{leadId}")
    public List<MessageDto> messages(Authentication authentication, @PathVariable Long leadId) {
        User current = me(authentication);
        Lead lead = leadService.scopedLead(current, leadId);
        return messageService.messagesFor(lead, current);
    }

    @PostMapping("/{leadId}")
    public ResponseEntity<MessageDto> send(Authentication authentication,
                                           @PathVariable Long leadId,
                                           @Valid @RequestBody SendMessageRequest request) {
        User current = me(authentication);
        Lead lead = leadService.scopedLead(current, leadId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messageService.send(lead, current, request.body()));
    }
}
