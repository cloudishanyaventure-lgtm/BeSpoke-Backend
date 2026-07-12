package com.BeSpoke.controller;

import com.BeSpoke.dto.SendMessageRequest;
import com.BeSpoke.entity.User;
import com.BeSpoke.service.ChatService;
import com.BeSpoke.service.CurrentUserService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * STOMP entrypoint. Clients connect to /ws, send to /app/threads/{id}/send
 * and subscribe to /topic/threads/{id}. The REST POST endpoint also broadcasts
 * to the same topic, so either path works.
 */
@Controller
public class ChatWsController {

    private final ChatService chatService;
    private final CurrentUserService currentUserService;

    public ChatWsController(ChatService chatService, CurrentUserService currentUserService) {
        this.chatService = chatService;
        this.currentUserService = currentUserService;
    }

    @MessageMapping("/threads/{id}/send")
    public void send(@DestinationVariable("id") Long threadId,
                     @Payload SendMessageRequest request,
                     Principal principal) {
        if (principal == null) {
            return; // unauthenticated STOMP sessions cannot send
        }
        User sender = currentUserService.requireByEmail(principal.getName());
        // sendMessage persists and broadcasts to /topic/threads/{id}
        chatService.sendMessage(sender, threadId, request.content());
    }
}
