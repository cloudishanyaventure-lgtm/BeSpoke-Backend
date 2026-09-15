package com.BeSpoke.service;

import com.BeSpoke.dto.AuthResponse;
import com.BeSpoke.dto.LoginRequest;
import com.BeSpoke.dto.RegisterRequest;
import com.BeSpoke.dto.UserDto;
import com.BeSpoke.entity.ActivityType;
import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.LeadActivity;
import com.BeSpoke.entity.LeadSource;
import com.BeSpoke.entity.LeadStatus;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.repository.CompanyRepository;
import com.BeSpoke.repository.LeadActivityRepository;
import com.BeSpoke.repository.LeadRepository;
import com.BeSpoke.repository.StaffProfileRepository;
import com.BeSpoke.repository.UserRepository;
import com.BeSpoke.security.GoogleTokenVerifier;
import com.BeSpoke.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Service

public class AuthService {

    /** Unambiguous alphabet: no I/l/1, no O/0, so a mailed password is readable. */
    private static final char[] PASSWORD_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".toCharArray();

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final LeadRepository leadRepository;
    private final LeadActivityRepository leadActivityRepository;
    private final CompanyRepository companyRepository;
    private final ScoreService scoreService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final StaffProfileRepository staffProfileRepository;
    private final MailService mailService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository,
                       LeadRepository leadRepository,
                       LeadActivityRepository leadActivityRepository,
                       CompanyRepository companyRepository,
                       ScoreService scoreService,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       StaffProfileRepository staffProfileRepository,
                       MailService mailService,
                       GoogleTokenVerifier googleTokenVerifier,
                       AuditService auditService) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.leadRepository = leadRepository;
        this.leadActivityRepository = leadActivityRepository;
        this.companyRepository = companyRepository;
        this.scoreService = scoreService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.staffProfileRepository = staffProfileRepository;
        this.mailService = mailService;
    }

    /**
     * Signing up IS the lead: User(CUSTOMER) + Lead(NEW_INQUIRY, WEBSITE) in one transaction.
     * The lead stays in the BeSpoke pool — the picked studio is only a preference (V3 §0) —
     * and the password is generated here and emailed, never chosen or returned (V3 §6).
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("An account with this email already exists");
        }
        String phone = userRepository.requireFreePhone(request.phone());
        String password = generatePassword();
        User user = new User(request.name().trim(), email,
                passwordEncoder.encode(password), Role.CUSTOMER);
        user.setPhone(phone);
        user.setCity(request.city().trim());
        user = userRepository.save(user);

        Lead lead = new Lead();
        lead.setCustomer(user);
        lead.setContactName(user.getName());
        lead.setContactEmail(user.getEmail());
        lead.setContactPhone(user.getPhone());
        lead.setCity(user.getCity());
        lead.setPropertyType(request.propertyType());
        lead.setBudgetBand(request.budgetBand());
        lead.setSource(LeadSource.WEBSITE);
        lead.setStatus(LeadStatus.NEW_INQUIRY);
        if (request.companyId() != null) {
            // A studio pick is a preference the platform honours when it routes; an id that
            // isn't a live verified design studio is simply ignored. No company, no transfer.
            companyRepository.findById(request.companyId())
                    .filter(Company::canTakeLeads)
                    .ifPresent(lead::setPreferredCompany);
        }
        scoreService.rescore(lead, null);
        lead = leadRepository.save(lead);

        leadActivityRepository.save(new LeadActivity(lead, null, ActivityType.SYSTEM,
                lead.getPreferredCompany() != null
                        ? "Lead created from website signup — prefers "
                                + lead.getPreferredCompany().getName()
                        : "Lead created from website signup"));

        mailService.customerSignedUp(user);
        // No token: the customer signs in with the mailed password (V3 §6).
        return new AuthResponse(null, UserDto.from(user), lead.getId());
    }

    /**
     * The account half of {@link #register} for a lead captured without one (walk-in,
     * phone, enquiry, email). Called when the funnel first moves: from CONTACTED onwards
     * the contact book, the portal, messages, drawings and invoices all need a CUSTOMER to
     * hang off. Returns null when there is nothing safe to mint — no email on the lead, or
     * the address already belongs to a staff account.
     */
    @Transactional
    public User customerForLead(Lead lead) {
        String email = lead.getContactEmail() == null ? "" : lead.getContactEmail().toLowerCase().trim();
        if (email.isEmpty()) {
            return null;
        }
        User existing = userRepository.findByEmail(email).orElse(null);
        if (existing != null) {
            // Same person enquiring twice gets one account; a staff address is left alone.
            return existing.getRole() == Role.CUSTOMER ? existing : null;
        }
        User customer = new User(lead.getContactName().trim(), email,
                passwordEncoder.encode(generatePassword()), Role.CUSTOMER);
        // A phone already held by another account is dropped rather than failing the stage
        // change — it stays readable on the lead either way.
        String phone = com.BeSpoke.repository.UserRepository.normalisePhone(lead.getContactPhone());
        if (phone != null && !userRepository.existsByPhone(phone)) {
            customer.setPhone(phone);
        }
        customer.setCity(lead.getCity());
        customer = userRepository.save(customer);
        mailService.customerSignedUp(customer);
        return customer;
    }

    private static String generatePassword() {
        StringBuilder password = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            password.append(PASSWORD_ALPHABET[RANDOM.nextInt(PASSWORD_ALPHABET.length)]);
        }
        return password.toString();
    }

    /** Partner sign-in: password is for staff only — homeowners use the OTP flow. */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        if (!user.isActive()) {
            throw new BadCredentialsException("Account is deactivated");
        }
        if (user.getRole() == Role.CUSTOMER) {
            throw new BadCredentialsException(
                    "This is the partner sign-in. Homeowners sign in with a one-time code.");
        }
        return session(user);
    }

    private static final Duration OTP_TTL = Duration.ofMinutes(10);

    /** A code issued less than this long ago is not reissued — stops mail-bombing an address. */
    private static final Duration OTP_REISSUE_AFTER = Duration.ofMinutes(1);

    /** The fixed code a review account signs in with. Never mailed, never expires. */
    private static final String REVIEW_CODE = "000000";

    /** How long a review account's fixed code stays valid — effectively forever. */
    private static final Duration REVIEW_CODE_TTL = Duration.ofDays(3650);

    /** Issues a fresh one-time code and mails it via {@code send}. */
    private void issueCode(User user, java.util.function.BiConsumer<User, String> send) {
        if (user.isInternal()) {
            // Store-review accounts: app-store reviewers cannot read our mail and their
            // credentials must survive every release, so the code is fixed and unmailed.
            user.setOtpCode(REVIEW_CODE);
            user.setOtpExpiresAt(Instant.now().plus(REVIEW_CODE_TTL));
            user.setOtpAttempts(0);
            userRepository.save(user);
            return;
        }
        if (user.getOtpExpiresAt() != null
                && user.getOtpExpiresAt().isAfter(Instant.now().plus(OTP_TTL).minus(OTP_REISSUE_AFTER))) {
            // Recently issued and still valid: silently keep the existing code, so the
            // generic 200 stays indistinguishable and SMTP quota can't be burned.
            return;
        }
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        user.setOtpCode(code);
        user.setOtpExpiresAt(Instant.now().plus(OTP_TTL));
        user.setOtpAttempts(0);
        userRepository.save(user);
        send.accept(user, code);
    }

    /**
     * Burns the stored code once it matches — single use, and burned after 5 wrong
     * guesses. Wrong, expired or burned code → one generic 400.
     */
    private User consumeCode(String email, String code, boolean staffSide) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .filter(User::isActive)
                .filter(u -> staffSide == (u.getRole() != Role.CUSTOMER))
                // A review account's code is fixed and always live, so it works even if the
                // app never called /otp/request first.
                .filter(u -> u.isInternal()
                        || (u.getOtpCode() != null
                        && u.getOtpExpiresAt() != null
                        && u.getOtpExpiresAt().isAfter(Instant.now())))
                .orElseThrow(() -> new BadRequestException("Invalid or expired code"));
        if (user.isInternal()) {
            if (!REVIEW_CODE.equals(code.trim())) {
                throw new BadRequestException("Invalid or expired code");
            }
            return user;  // never burned, never counted — the reviewer signs in again next release
        }
        if (!user.getOtpCode().equals(code.trim())) {
            user.setOtpAttempts(user.getOtpAttempts() + 1);
            if (user.getOtpAttempts() >= 5) {
                user.setOtpCode(null);
                user.setOtpExpiresAt(null);
            }
            userRepository.save(user);
            throw new BadRequestException("Invalid or expired code");
        }
        user.setOtpCode(null);
        user.setOtpExpiresAt(null);
        user.setOtpAttempts(0);
        return userRepository.save(user);
    }

    /**
     * Homeowner sign-in. Silently does nothing for unknown/inactive emails or for staff
     * (they sign in with a password) — the controller always answers with the same generic
     * message, so the endpoint cannot be used to probe accounts.
     */
    @Transactional
    public void requestOtp(String email) {
        userRepository.findByEmail(email.toLowerCase().trim())
                .filter(User::isActive)
                .filter(u -> u.getRole() == Role.CUSTOMER)
                .ifPresent(user -> issueCode(user, mailService::loginOtp));
    }

    /**
     * Single use: the code is cleared on success, and burned after 5 wrong guesses.
     * Wrong, expired or burned code → one generic 400.
     */
    @Transactional
    public AuthResponse verifyOtp(String email, String code) {
        return session(consumeCode(email, code, false));
    }

    /**
     * Partner "forgot password": mails a code to staff only. Customers have no password
     * to reset — they sign in with a code — and unknown emails are silently ignored, so
     * the generic 200 from the controller gives nothing away either way.
     */
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(email.toLowerCase().trim())
                .filter(User::isActive)
                .filter(u -> u.getRole() != Role.CUSTOMER)
                .ifPresent(user -> issueCode(user, mailService::passwordResetCode));
    }

    /** Code verified and burned, new password stored, and they're signed straight in. */
    @Transactional
    public AuthResponse resetPassword(String email, String code, String newPassword) {
        User user = consumeCode(email, code, true);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        // Truncated to seconds because JWT iat is second-precision: the session issued
        // below must survive its own reset while every earlier token dies.
        user.setCredentialsChangedAt(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        user = userRepository.save(user);
        mailService.passwordChanged(user);
        return session(user);
    }

    /**
     * DPDP account deletion, step 1: mail a confirmation code. Customer accounts only —
     * staff accounts belong to their company and are managed by its director. Silent for
     * unknown emails, same as requestOtp, so the endpoint can't probe accounts.
     */
    @Transactional
    public void requestAccountDeletion(String email) {
        userRepository.findByEmail(email.toLowerCase().trim())
                .filter(User::isActive)
                .filter(u -> u.getRole() == Role.CUSTOMER)
                .ifPresent(user -> issueCode(user, mailService::accountDeletionCode));
    }

    /**
     * Step 2: code verified → the login is gone for good. Live personal data (name,
     * email, phone, city, avatar) is erased and the address freed for a future signup;
     * leads/invoices/messages keep their own business-record snapshots. Any session
     * still holding a token dies via credentialsChangedAt.
     */
    @Transactional
    public void confirmAccountDeletion(String email, String code) {
        User user = consumeCode(email, code, false);
        String originalEmail = user.getEmail();
        String originalName = user.getName();
        user.setActive(false);
        user.setName("Deleted customer");
        user.setEmail("deleted-" + user.getId() + "@removed.bespokedesign.in");
        user.setPhone(null);
        user.setCity(null);
        user.setAvatarUrl(null);
        user.setCredentialsChangedAt(Instant.now());
        userRepository.save(user);
        auditService.log(user, null, "ACCOUNT_DELETED",
                "Customer account #" + user.getId() + " deleted at the owner's request");
        mailService.accountDeleted(originalEmail, originalName);
    }

    /**
     * Google sign-in for both sides. The email Google confirms must already have an
     * account on the side it was asked from — we never create one here, so the caller
     * gets a clear "no account" message and sends them to sign up / apply.
     */
    public AuthResponse loginWithGoogle(String credential, boolean partnerDoor) {
        String email = googleTokenVerifier.verifiedEmail(credential);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException(NO_GOOGLE_ACCOUNT));
        if (!user.isActive()) {
            throw new BadCredentialsException("Account is deactivated");
        }
        if (partnerDoor != (user.getRole() != Role.CUSTOMER)) {
            throw new BadRequestException(partnerDoor
                    ? "That Google account is a homeowner account — sign in on the main site."
                    : "That's a partner account — sign in on the partner site.");
        }
        return session(user);
    }

    /** The message the frontend keys on to offer "create an account". */
    public static final String NO_GOOGLE_ACCOUNT = "No BeSpoke account uses that Google email";

    private AuthResponse session(User user) {
        // The sidebar shows the designation under the name (V3 §12); customers have no profile.
        String title = staffProfileRepository.findByUser(user)
                .map(com.BeSpoke.entity.StaffProfile::getTitle).orElse(null);
        return new AuthResponse(jwtService.generateToken(user), UserDto.from(user, title), null);
    }
}
