package com.BeSpoke.service;

import com.BeSpoke.dto.ChatMessageDto;
import com.BeSpoke.dto.ChatThreadDto;
import com.BeSpoke.entity.ChatMessage;
import com.BeSpoke.entity.ChatThread;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.ForbiddenException;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.ChatMessageRepository;
import com.BeSpoke.repository.ChatThreadRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatService {

    private final ChatThreadRepository chatThreadRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatService(ChatThreadRepository chatThreadRepository,
                       ChatMessageRepository chatMessageRepository,
                       SimpMessagingTemplate messagingTemplate) {
        this.chatThreadRepository = chatThreadRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional(readOnly = true)
    public List<ChatThreadDto> myThreads(User user) {
        return chatThreadRepository.findByCustomerIdOrDesignerIdOrderByCreatedAtDesc(user.getId(), user.getId())
                .stream().map(ChatThreadDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> messages(User user, Long threadId) {
        requireParticipant(user, requireThread(threadId));
        return chatMessageRepository.findByThreadIdOrderBySentAtAsc(threadId)
                .stream().map(ChatMessageDto::from).toList();
    }

    /** Persists the message and broadcasts it to /topic/threads/{id}. */
    @Transactional
    public ChatMessageDto sendMessage(User sender, Long threadId, String content) {
        ChatThread thread = requireThread(threadId);
        requireParticipant(sender, thread);
        ChatMessage message = chatMessageRepository.save(new ChatMessage(thread, sender, content));
        ChatMessageDto dto = ChatMessageDto.from(message);
        messagingTemplate.convertAndSend("/topic/threads/" + threadId, dto);
        return dto;
    }

    private ChatThread requireThread(Long threadId) {
        return chatThreadRepository.findById(threadId)
                .orElseThrow(() -> new NotFoundException("Chat thread not found: " + threadId));
    }

    private void requireParticipant(User user, ChatThread thread) {
        boolean participant = thread.getCustomer().getId().equals(user.getId())
                || thread.getDesigner().getId().equals(user.getId())
                || user.getRole() == Role.ADMIN;
        if (!participant) {
            throw new ForbiddenException("You are not a participant of this chat thread");
        }
    }
}
