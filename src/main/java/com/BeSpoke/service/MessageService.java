package com.BeSpoke.service;

import com.BeSpoke.dto.MessageDto;
import com.BeSpoke.dto.ThreadDto;
import com.BeSpoke.dto.UserRefDto;
import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.LeadStatus;
import com.BeSpoke.entity.Message;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.User;
import com.BeSpoke.repository.LeadRepository;
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
    private final LeadRepository leadRepository;

    public MessageService(MessageRepository messageRepository, LeadRepository leadRepository) {
        this.messageRepository = messageRepository;
        this.leadRepository = leadRepository;
    }

    /** Returns the thread and marks the other side's messages as read for the current user. */
    @Transactional
    public List<MessageDto> messagesFor(Lead lead, User current) {
        for (Message message : messageRepository.findByLeadAndSenderNotAndReadAtIsNull(lead, current)) {
            message.setReadAt(Instant.now());
            messageRepository.save(message);
        }
        return messageRepository.findByLeadOrderByCreatedAtAsc(lead)
                .stream().map(MessageDto::from).toList();
    }

    @Transactional
    public MessageDto send(Lead lead, User sender, String body) {
        Message message = messageRepository.save(new Message(lead, sender, body.trim()));
        return MessageDto.from(message);
    }

    /** Staff thread list: leads that have messages OR are still open (not LOST), scoped for designers. */
    public List<ThreadDto> threads(User current) {
        List<Lead> leads = current.getRole() == Role.DESIGNER
                ? leadRepository.findByAssignedDesignerOrderByCreatedAtDesc(current)
                : leadRepository.findAllByOrderByCreatedAtDesc();
        List<ThreadDto> threads = new ArrayList<>();
        for (Lead lead : leads) {
            Message last = messageRepository.findFirstByLeadOrderByCreatedAtDesc(lead).orElse(null);
            if (last == null && lead.getStatus() == LeadStatus.LOST) {
                continue;
            }
            threads.add(new ThreadDto(
                    lead.getId(),
                    lead.getContactName(),
                    lead.getStatus().name(),
                    UserRefDto.from(lead.getAssignedDesigner()),
                    last != null ? last.getBody() : null,
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
}
