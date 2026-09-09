package com.BeSpoke.config;

import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.User;
import com.BeSpoke.repository.*;
import com.BeSpoke.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductionBootstrapTest {
    final UserRepository users = mock(UserRepository.class);
    final CompanyRepository companies = mock(CompanyRepository.class);
    final RoomCatalogItemRepository catalog = mock(RoomCatalogItemRepository.class);
    final PasswordEncoder passwords = mock(PasswordEncoder.class);
    final SeedRunner runner = new SeedRunner(users, mock(StaffProfileRepository.class), catalog, companies,
            mock(LeadRepository.class), mock(ProductRepository.class), passwords, new ObjectMapper(),
            mock(JdbcTemplate.class), mock(PlatformOptionService.class), mock(PolicyService.class));

    void configure() {
        ReflectionTestUtils.setField(runner, "demoEnabled", false);
        ReflectionTestUtils.setField(runner, "bootstrapEmail", " Owner@Example.com ");
        ReflectionTestUtils.setField(runner, "bootstrapPassword", "test-password-at-least-16");
        when(catalog.count()).thenReturn(1L);
        when(passwords.encode(anyString())).thenReturn("hashed-password");
        when(users.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }
    @Test void freshProductionCreatesOnlyConfiguredAdministrator() throws Exception {
        configure(); runner.run();
        var captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(users, times(1)).save(captor.capture());
        assertEquals("owner@example.com", captor.getValue().getEmail());
        assertEquals(Role.SUPER_ADMIN, captor.getValue().getRole());
        verify(passwords).encode("test-password-at-least-16");
        verifyNoInteractions(companies);
    }
    @Test void subsequentStartupDoesNotResetExistingAdmin() throws Exception {
        configure(); when(users.countByRoleAndActiveTrue(Role.SUPER_ADMIN)).thenReturn(1L);
        ReflectionTestUtils.setField(runner, "bootstrapPassword", "");
        runner.run(); verify(users, never()).save(any()); verifyNoInteractions(passwords, companies);
    }
    @Test void bootstrapCannotPromoteAnExistingCustomer() {
        configure(); when(users.existsByEmail("owner@example.com")).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> runner.run()); verify(users, never()).save(any());
    }
}
