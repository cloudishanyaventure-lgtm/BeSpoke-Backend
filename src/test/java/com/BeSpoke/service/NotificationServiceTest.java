package com.BeSpoke.service;
import com.BeSpoke.entity.*;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.AppNotificationRepository;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
class NotificationServiceTest {
    final AppNotificationRepository repo = mock(AppNotificationRepository.class);
    final NotificationService service = new NotificationService(repo);
    final User customer = new User("Customer", "customer@example.com", "hash", Role.CUSTOMER);
    @Test void anotherUsersNotificationCannotBeRead() {
        when(repo.findByIdAndRecipient(1L, customer)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.read(customer, 1L)); verify(repo, never()).save(any());
    }
    @Test void readingAnAlreadyReadNotificationPreservesItsTimestamp() {
        AppNotification n = new AppNotification(customer, "Title", "Body", "/my");
        Instant read = Instant.parse("2026-09-01T00:00:00Z"); n.setReadAt(read);
        when(repo.findByIdAndRecipient(1L, customer)).thenReturn(Optional.of(n)); when(repo.save(any())).thenReturn(n);
        assertEquals(read, service.read(customer, 1L).readAt());
    }
    @Test void markAllReadIsScopedToCurrentUser() {
        service.readAll(customer); verify(repo).markAllRead(eq(customer), any());
    }
    @Test void walkInLeadsDoNotCreateAnOrphanNotification() {
        service.publish(null, "Title", "Body", "/my"); verifyNoInteractions(repo);
    }
}
