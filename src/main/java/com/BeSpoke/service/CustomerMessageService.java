package com.BeSpoke.service;

import com.BeSpoke.dto.CustomerConversationDto;
import com.BeSpoke.dto.MessageDto;
import com.BeSpoke.dto.UserRefDto;
import com.BeSpoke.entity.*;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.LeadRepository;
import com.BeSpoke.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CustomerMessageService {
    private final LeadRepository leads;
    private final MessageRepository messages;
    private final CryptoService crypto;

    public CustomerMessageService(LeadRepository leads, MessageRepository messages, CryptoService crypto) {
        this.leads = leads;
        this.messages = messages;
        this.crypto = crypto;
    }

    public List<CustomerConversationDto> conversations(User current, Long leadId) {
        Lead selected = leads.findById(leadId).filter(lead -> canRead(current, lead))
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        List<Lead> projects = selected.getCustomer() == null ? List.of(selected)
                : leads.findByCustomerOrderByCreatedAtDesc(selected.getCustomer());
        return projects.stream().filter(lead -> canRead(current, lead))
                .map(lead -> new CustomerConversationDto(lead.getId(), lead.getContactName(),
                        UserRefDto.from(lead.getAssignedDesigner()),
                        messages.findByLeadOrderByCreatedAtAsc(lead).stream()
                                .map(message -> MessageDto.from(message, crypto.decrypt(message.getBody())))
                                .toList()))
                // Projects with no messages are kept: the tab can start a conversation, not just read one.
                .toList();
    }

    private boolean canRead(User current, Lead lead) {
        return current.getRole().isPlatform() || (current.getRole().isStaff()
                && current.getCompany() != null && lead.getCompany() != null
                && current.getCompany().getType() == CompanyType.DESIGN
                && current.getCompany().getId().equals(lead.getCompany().getId()));
    }
}
