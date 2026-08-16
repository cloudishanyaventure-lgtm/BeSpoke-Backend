package com.BeSpoke.service;

import com.BeSpoke.dto.LoginRequest;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.repository.StaffProfileRepository;
import com.BeSpoke.repository.UserRepository;
import com.BeSpoke.security.GoogleTokenVerifier;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Two front doors: homeowners sign in with a mailed code, partners with a password
 * (a code only ever resets theirs). Neither door opens for the other side.
 */
class AuthServiceLoginSeparationTest {

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final JwtService jwt = mock(JwtService.class);
    private final StaffProfileRepository profiles = mock(StaffProfileRepository.class);
    private final MailService mail = mock(MailService.class);
    private final GoogleTokenVerifier google = mock(GoogleTokenVerifier.class);

    private final AuthService auth = new AuthService(users, null, null, null, null,
            encoder, jwt, profiles, mail, google);

    private User user(Role role) {
        User u = new User("Someone", "someone@bespoke.in", "hash", role);
        when(users.findByEmail("someone@bespoke.in")).thenReturn(Optional.of(u));
        when(users.save(any())).thenAnswer(call -> call.getArgument(0));
        when(encoder.matches(anyString(), anyString())).thenReturn(true);
        when(encoder.encode(anyString())).thenReturn("new-hash");
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
    void staffGetNoLoginCode() {
        user(Role.DESIGNER);
        auth.requestOtp("someone@bespoke.in");
        verify(mail, never()).loginOtp(any(), anyString());
        assertThrows(BadRequestException.class,
                () -> auth.verifyOtp("someone@bespoke.in", "123456"));
    }

    @Test
    void customersGetALoginCode() {
        User customer = user(Role.CUSTOMER);
        auth.requestOtp("someone@bespoke.in");
        verify(mail).loginOtp(any(), anyString());
        assertEquals("token",
                auth.verifyOtp("someone@bespoke.in", customer.getOtpCode()).token());
    }

    @Test
    void customersGetNoResetCode() {
        user(Role.CUSTOMER);
        auth.forgotPassword("someone@bespoke.in");
        verify(mail, never()).passwordResetCode(any(), anyString());
    }

    @Test
    void staffResetTheirPasswordWithAMailedCode() {
        User staff = user(Role.DESIGNER);
        auth.forgotPassword("someone@bespoke.in");
        verify(mail).passwordResetCode(any(), anyString());
        String code = staff.getOtpCode();

        assertEquals("token",
                auth.resetPassword("someone@bespoke.in", code, "brand-new-pass").token());
        assertEquals("new-hash", staff.getPasswordHash());
    }

    @Test
    void aResetCodeIsSingleUse() {
        User staff = user(Role.DESIGNER);
        auth.forgotPassword("someone@bespoke.in");
        String code = staff.getOtpCode();
        auth.resetPassword("someone@bespoke.in", code, "brand-new-pass");
        assertThrows(BadRequestException.class,
                () -> auth.resetPassword("someone@bespoke.in", code, "another-pass"));
    }

    @Test
    void googleOnlyOpensTheSideItsAccountBelongsTo() {
        user(Role.DESIGNER);
        when(google.verifiedEmail("id-token")).thenReturn("someone@bespoke.in");

        assertEquals("token", auth.loginWithGoogle("id-token", true).token());
        assertThrows(BadRequestException.class, () -> auth.loginWithGoogle("id-token", false));
    }

    @Test
    void googleRefusesAnEmailWithNoAccount() {
        when(google.verifiedEmail("id-token")).thenReturn("stranger@example.com");
        when(users.findByEmail("stranger@example.com")).thenReturn(Optional.empty());
        assertEquals(AuthService.NO_GOOGLE_ACCOUNT,
                assertThrows(BadRequestException.class,
                        () -> auth.loginWithGoogle("id-token", false)).getMessage());
    }
}
