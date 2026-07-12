package com.BeSpoke.controller;

import com.BeSpoke.dto.ChatMessageDto;
import com.BeSpoke.dto.ChatThreadDto;
import com.BeSpoke.dto.SendMessageRequest;
import com.BeSpoke.entity.User;
import com.BeSpoke.service.ChatService;
import com.BeSpoke.service.CurrentUserService;
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

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final CurrentUserService currentUserService;

    public ChatController(ChatService chatService, CurrentUserService currentUserService) {
        this.chatService = chatService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/threads")
    public List<ChatThreadDto> myThreads(Authentication authentication) {
        User user = currentUserService.requireByEmail(authentication.getName());
        return chatService.myThreads(user);
    }

    @GetMapping("/threads/{id}/messages")
    public List<ChatMessageDto> messages(Authentication authentication, @PathVariable Long id) {
        User user = currentUserService.requireByEmail(authentication.getName());
        return chatService.messages(user, id);
    }

    @PostMapping("/threads/{id}/messages")
    public ResponseEntity<ChatMessageDto> send(Authentication authentication,
                                               @PathVariable Long id,
                                               @Valid @RequestBody SendMessageRequest request) {
        User user = currentUserService.requireByEmail(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.sendMessage(user, id, request.content()));
    }
}
