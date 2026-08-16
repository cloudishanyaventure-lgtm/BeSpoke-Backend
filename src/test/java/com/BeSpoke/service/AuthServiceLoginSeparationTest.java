package com.BeSpoke.service;

import com.BeSpoke.dto.LoginRequest;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.repository.StaffProfileRepository;
import com.BeSpoke.repository.UserRepository;
import com.BeSpoke.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Two front doors, both one-time-code (partners may also use their mailed password).
 * Neither door opens for the other side.
 */
class AuthServiceLoginSeparationTest {

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final JwtService jwt = mock(JwtService.class);
    private final StaffProfileRepository profiles = mock(StaffProfileRepository.class);
    private final MailService mail = mock(MailService.class);

    private final AuthService auth = new AuthService(users, null, null, null, null,
            encoder, jwt, profiles, mail);

    private User user(Role role) {
        User u = new User("Someone", "someone@bespoke.in", "hash", role);
        when(users.findByEmail("someone@bespoke.in")).thenReturn(Optional.of(u));
        when(encoder.matches(anyString(), anyString())).thenReturn(true);
        when(profiles.findByUser(any())).thenReturn(Optional.empty());
        when(jwt.generateToken(any())).thenReturn("token");
        return u;
    }

    private LoginRequest password() {
        return new LoginRequest("someone@bespoke.in", "secret");
    }

    @Test
    void partnerSignsInWithPassword() {
        user(Role.DESIGNER);
        assertEquals("token", auth.login(password()).token());
    }

    @Test
    void customerCannotUseThePartnerSignIn() {
        user(Role.CUSTOMER);
        assertThrows(BadCredentialsException.class, () -> auth.login(password()));
    }

    @Test
    void staffGetNoCodeFromTheHomeownerDoor() {
        user(Role.DESIGNER);
        auth.requestOtp("someone@bespoke.in", false);
        verify(mail, never()).loginOtp(any(), anyString());
        assertThrows(BadRequestException.class,
                () -> auth.verifyOtp("someone@bespoke.in", "123456", false));
    }

    @Test
    void customersGetNoCodeFromThePartnerDoor() {
        user(Role.CUSTOMER);
        auth.requestOtp("someone@bespoke.in", true);
        verify(mail, never()).loginOtp(any(), anyString());
        assertThrows(BadRequestException.class,
                () -> auth.verifyOtp("someone@bespoke.in", "123456", true));
    }

    @Test
    void eachSideGetsACodeFromItsOwnDoor() {
        user(Role.CUSTOMER);
        auth.requestOtp("someone@bespoke.in", false);
        user(Role.DESIGNER);
        auth.requestOtp("someone@bespoke.in", true);
        verify(mail, times(2)).loginOtp(any(), anyString());
    }

    /** A code mailed to a partner still only verifies at the partner door. */
    @Test
    void aCodeOnlyWorksAtTheDoorItWasAskedFrom() {
        User staff = user(Role.DESIGNER);
        auth.requestOtp("someone@bespoke.in", true);
        String code = staff.getOtpCode();
        assertThrows(BadRequestException.class,
                () -> auth.verifyOtp("someone@bespoke.in", code, false));
        assertEquals("token", auth.verifyOtp("someone@bespoke.in", code, true).token());
    }
}
