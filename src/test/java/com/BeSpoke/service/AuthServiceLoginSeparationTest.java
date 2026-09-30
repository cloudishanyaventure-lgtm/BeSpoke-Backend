package com.BeSpoke.service;

import com.BeSpoke.dto.LoginRequest;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.entity.EmailVerification;
import com.BeSpoke.repository.EmailVerificationRepository;
import com.BeSpoke.repository.StaffProfileRepository;
import com.BeSpoke.repository.UserRepository;
import com.BeSpoke.security.GoogleTokenVerifier;
import com.BeSpoke.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    private final EmailVerificationRepository verifications = mock(EmailVerificationRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final JwtService jwt = mock(JwtService.class);
    private final StaffProfileRepository profiles = mock(StaffProfileRepository.class);
    private final MailService mail = mock(MailService.class);
    private final GoogleTokenVerifier google = mock(GoogleTokenVerifier.class);

    private final AuditService audit = mock(AuditService.class);

    private final AuthService auth = new AuthService(users, verifications, null, null, null, null,
            encoder, jwt, profiles, mail, mock(WhatsAppService.class), google, audit);

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
    void aReviewAccountSignsInWithTheFixedCodeAndIsNeverMailed() {
        User reviewer = user(Role.CUSTOMER);
        reviewer.setInternal(true);

        // Works with no /otp/request call at all — the code is always live.
        assertEquals("token", auth.verifyOtp("someone@bespoke.in", "000000").token());

        auth.requestOtp("someone@bespoke.in");
        verify(mail, never()).loginOtp(any(), anyString());
        assertEquals("000000", reviewer.getOtpCode());

        // Reusable across releases: signing in does not burn it.
        assertEquals("token", auth.verifyOtp("someone@bespoke.in", "000000").token());
        assertEquals("000000", reviewer.getOtpCode());
        // Any other code is still rejected.
        assertThrows(BadRequestException.class,
                () -> auth.verifyOtp("someone@bespoke.in", "123456"));
    }

    @Test
    void aNormalCustomerCannotUseTheFixedCode() {
        user(Role.CUSTOMER);  // not flagged, no code issued
        assertThrows(BadRequestException.class,
                () -> auth.verifyOtp("someone@bespoke.in", "000000"));
    }

    /**
     * The signup door: a code goes to an address with no account, and the right one says
     * the address is verified and free rather than opening a session — which is what puts
     * the visitor into onboarding instead of a dead end.
     */
    @Test
    void anAddressWithNoAccountGetsACodeAndComesBackAsANewAccount() {
        when(users.findByEmail("nobody@home.test")).thenReturn(Optional.empty());
        when(verifications.findByEmail("nobody@home.test")).thenReturn(Optional.empty());
        var saved = org.mockito.ArgumentCaptor.forClass(EmailVerification.class);
        when(verifications.save(saved.capture())).thenAnswer(call -> call.getArgument(0));

        auth.startOtp("nobody@home.test");
        verify(mail).signupOtp(eq("nobody@home.test"), anyString());
        EmailVerification pending = saved.getValue();

        // The stored code is what was mailed, and it is the only one that works.
        when(verifications.findByEmail("nobody@home.test")).thenReturn(Optional.of(pending));
        assertThrows(BadRequestException.class,
                () -> auth.confirmOtp("nobody@home.test", "000000"));

        var answer = auth.confirmOtp("nobody@home.test", pending.getCode());
        assertTrue(answer.newAccount(), "verified, but there is nothing to sign in to yet");
        assertNull(answer.token());
    }

    /** Five wrong guesses kill the code, same as a sign-in code. */
    @Test
    void aSignupCodeBurnsAfterFiveWrongGuesses() {
        when(users.findByEmail("nobody@home.test")).thenReturn(Optional.empty());
        EmailVerification pending = new EmailVerification("nobody@home.test", "654321",
                java.time.Instant.now().plusSeconds(600));
        when(verifications.findByEmail("nobody@home.test")).thenReturn(Optional.of(pending));
        when(verifications.save(any())).thenAnswer(call -> call.getArgument(0));

        for (int i = 0; i < 5; i++) {
            assertThrows(BadRequestException.class,
                    () -> auth.confirmOtp("nobody@home.test", "000000"));
        }
        // Even the right code is no good now.
        assertThrows(BadRequestException.class,
                () -> auth.confirmOtp("nobody@home.test", "654321"));
    }

    /**
     * A partner typing their address into the homeowner door must not be handed a signup
     * code — that would walk them into creating a second account they cannot use.
     */
    @Test
    void aStaffAddressIsNeverGivenASignupCode() {
        user(Role.DESIGNER);
        auth.startOtp("someone@bespoke.in");
        verify(mail, never()).signupOtp(anyString(), anyString());
        verify(mail, never()).loginOtp(any(), anyString());
        verify(verifications, never()).save(any());
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
    void aPasswordResetEvictsOlderSessions() {
        User staff = user(Role.DESIGNER);
        auth.forgotPassword("someone@bespoke.in");
        auth.resetPassword("someone@bespoke.in", staff.getOtpCode(), "brand-new-pass");
        // Stamped, and truncated to seconds so the fresh session's own second-precision
        // iat survives its own reset.
        org.junit.jupiter.api.Assertions.assertNotNull(staff.getCredentialsChangedAt());
        assertEquals(0, staff.getCredentialsChangedAt().getNano());
    }

    @Test
    void aFreshCodeIsNotReissuedWithinAMinute() {
        User customer = user(Role.CUSTOMER);
        auth.requestOtp("someone@bespoke.in");
        String first = customer.getOtpCode();
        auth.requestOtp("someone@bespoke.in");
        assertEquals(first, customer.getOtpCode());
        verify(mail, org.mockito.Mockito.times(1)).loginOtp(any(), anyString());
    }

    @Test
    void accountDeletionAnonymisesAndDeactivates() {
        User customer = user(Role.CUSTOMER);
        auth.requestAccountDeletion("someone@bespoke.in");
        verify(mail).accountDeletionCode(any(), anyString());
        auth.confirmAccountDeletion("someone@bespoke.in", customer.getOtpCode());

        org.junit.jupiter.api.Assertions.assertFalse(customer.isActive());
        org.junit.jupiter.api.Assertions.assertNull(customer.getPhone());
        org.junit.jupiter.api.Assertions.assertTrue(
                customer.getEmail().startsWith("deleted-"));
        org.junit.jupiter.api.Assertions.assertNotNull(customer.getCredentialsChangedAt());
        // The goodbye mail goes to the address they signed up with, not the tombstone.
        verify(mail).accountDeleted("someone@bespoke.in", "Someone");
    }

    @Test
    void staffCannotDeleteTheirAccountThemselves() {
        user(Role.DESIGNER);
        auth.requestAccountDeletion("someone@bespoke.in");
        verify(mail, never()).accountDeletionCode(any(), anyString());
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
