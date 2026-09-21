package com.BeSpoke.service;
import com.BeSpoke.entity.*;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerMessageServiceTest {
    final LeadRepository leads = mock(LeadRepository.class);
    final MessageRepository messages = mock(MessageRepository.class);
    final CryptoService crypto = mock(CryptoService.class);
    final CustomerMessageService service = new CustomerMessageService(leads, messages, crypto);
    Company company(long id) {
        Company c = mock(Company.class);
        when(c.getId()).thenReturn(id);
        when(c.getType()).thenReturn(CompanyType.DESIGN);
        return c;
    }
    User user(Role role, Company company) {
        User u = mock(User.class);
        when(u.getRole()).thenReturn(role);
        when(u.getCompany()).thenReturn(company);
        return u;
    }
    Lead lead(long id, Company company, User customer) {
        Lead l = mock(Lead.class);
        when(l.getId()).thenReturn(id);
        when(l.getCompany()).thenReturn(company);
        when(l.getCustomer()).thenReturn(customer);
        return l;
    }
    @Test void allStudioRolesSeeOtherDesignersHistoryWithoutMarkingRead() {
        Company studio = company(1L);
        User customer = user(Role.CUSTOMER, null);
        Lead first = lead(3L, studio, customer), second = lead(4L, studio, customer);
        Lead foreign = lead(5L, company(2L), customer);
        when(leads.findById(3L)).thenReturn(Optional.of(first));
        when(leads.findByCustomerOrderByCreatedAtDesc(customer)).thenReturn(List.of(first, second, foreign));
        Message m = mock(Message.class);
        when(m.getLead()).thenReturn(second);
        when(m.getSender()).thenReturn(customer);
        when(m.getBody()).thenReturn("ciphertext");
        when(crypto.decrypt("ciphertext")).thenReturn("Hello");
        when(messages.findByLeadOrderByCreatedAtAsc(first)).thenReturn(List.of());
        when(messages.findByLeadOrderByCreatedAtAsc(second)).thenReturn(List.of(m));
        for (Role role : Role.applicableTo(CompanyType.DESIGN)) {
            var result = service.conversations(user(role, studio), 3L);
            // Both same-studio projects, including the silent one — staff can open a chat on it.
            assertEquals(List.of(3L, 4L), result.stream().map(t -> t.leadId()).sorted().toList());
            var withMessages = result.stream().filter(t -> t.leadId() == 4L).findFirst().orElseThrow();
            assertEquals("Hello", withMessages.messages().get(0).body());
        }
        verify(messages, never()).findByLeadOrderByCreatedAtAsc(foreign);
        verify(messages, never()).save(any());
        verify(m, never()).setReadAt(any());
    }
    @Test void deniesCustomersAndOtherStudios() {
        Company studio = company(1L);
        Lead selected = lead(3L, studio, null);
        when(leads.findById(3L)).thenReturn(Optional.of(selected));
        assertThrows(NotFoundException.class, () -> service.conversations(user(Role.CUSTOMER, studio), 3L));
        assertThrows(NotFoundException.class, () -> service.conversations(user(Role.DESIGNER, company(2L)), 3L));
        verifyNoInteractions(messages);
    }
    @Test void unlinkedContactDoesNotMatchOtherContacts() {
        Lead selected = lead(3L, null, null);
        when(leads.findById(3L)).thenReturn(Optional.of(selected));
        when(messages.findByLeadOrderByCreatedAtAsc(selected)).thenReturn(List.of());
        var result = service.conversations(user(Role.ADMIN, null), 3L);
        assertEquals(List.of(3L), result.stream().map(t -> t.leadId()).toList());
        assertTrue(result.get(0).messages().isEmpty());
        verify(leads, never()).findByCustomerOrderByCreatedAtDesc(any());
    }
}
