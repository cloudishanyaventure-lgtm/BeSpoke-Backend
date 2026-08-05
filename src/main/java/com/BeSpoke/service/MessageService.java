package com.BeSpoke.service;

import com.BeSpoke.dto.MessageDto;
import com.BeSpoke.dto.ThreadDto;
import com.BeSpoke.dto.UserRefDto;
import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.LeadStatus;
import com.BeSpoke.entity.Message;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.ConflictException;
import com.BeSpoke.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;
    private final LeadService leadService;
    private final CryptoService cryptoService;

    public MessageService(MessageRepository messageRepository,
                          LeadService leadService,
                          CryptoService cryptoService) {
        this.messageRepository = messageRepository;
        this.leadService = leadService;
        this.cryptoService = cryptoService;
    }

    /** Returns the thread and marks the other side's messages as read for the current user. */
    @Transactional
    public List<MessageDto> messagesFor(Lead lead, User current) {
        requireAccepted(lead);
        for (Message message : messageRepository.findByLeadAndSenderNotAndReadAtIsNull(lead, current)) {
            message.setReadAt(Instant.now());
            messageRepository.save(message);
        }
        return messageRepository.findByLeadOrderByCreatedAtAsc(lead).stream()
                .map(message -> MessageDto.from(message, cryptoService.decrypt(message.getBody())))
                .toList();
    }

    @Transactional
    public MessageDto send(Lead lead, User sender, String body) {
        requireAccepted(lead);
        String plain = body.trim();
        Message message = messageRepository.save(new Message(lead, sender, cryptoService.encrypt(plain)));
        return MessageDto.from(message, plain);
    }

    /**
     * Staff thread list: accepted leads that have messages OR are still open (not LOST),
     * company/role scoped. Unaccepted leads have no thread yet (§8a).
     */
    public List<ThreadDto> threads(User current) {
        List<Lead> leads = leadService.visibleLeads(current);
        List<ThreadDto> threads = new ArrayList<>();
        for (Lead lead : leads) {
            if (lead.getAcceptedAt() == null) {
                continue;
            }
            Message last = messageRepository.findFirstByLeadOrderByCreatedAtDesc(lead).orElse(null);
            if (last == null && lead.getStatus() == LeadStatus.LOST) {
                continue;
            }
            threads.add(new ThreadDto(
                    lead.getId(),
                    lead.getContactName(),
                    lead.getStatus().name(),
                    UserRefDto.from(lead.getAssignedDesigner()),
                    last != null ? cryptoService.decrypt(last.getBody()) : null,
                    last != null ? last.getSender().getName() : null,
                    last != null ? last.getCreatedAt() : null,
                    messageRepository.countByLeadAndSenderNotAndReadAtIsNull(lead, current)));
        }
        threads.sort((a, b) -> {
            Instant ta = a.lastAt();
            Instant tb = b.lastAt();
            if (ta == null && tb == null) {
                return Long.compare(b.leadId(), a.leadId());
            }
            if (ta == null) {
                return 1;
            }
            if (tb == null) {
                return -1;
            }
            return tb.compareTo(ta);
        });
        return threads;
    }

    /**
     * One guard for every route — staff (MessageController) and the customer portal
     * (MyController) both land here, so messaging cannot open before a studio accepts.
     */
    private void requireAccepted(Lead lead) {
        if (lead.getAcceptedAt() == null) {
            throw new ConflictException("Messaging opens once a studio accepts this project");
        }
    }
}
