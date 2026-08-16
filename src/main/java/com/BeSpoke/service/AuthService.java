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

    public AuthService(UserRepository userRepository,
                       LeadRepository leadRepository,
                       LeadActivityRepository leadActivityRepository,
                       CompanyRepository companyRepository,
                       ScoreService scoreService,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       StaffProfileRepository staffProfileRepository,
                       MailService mailService) {
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
        // The sidebar shows the designation under the name (V3 §12); customers have no profile.
        String title = staffProfileRepository.findByUser(user)
                .map(com.BeSpoke.entity.StaffProfile::getTitle).orElse(null);
        return new AuthResponse(jwtService.generateToken(user), UserDto.from(user, title), null);
    }

    private static final Duration OTP_TTL = Duration.ofMinutes(10);

    /** Each sign-in page only ever serves its own side: homeowners vs partner staff. */
    private static boolean belongsAt(User user, boolean partnerDoor) {
        return partnerDoor == (user.getRole() != Role.CUSTOMER);
    }

    /**
     * Silently does nothing for unknown/inactive emails, or an email that belongs at the
     * other sign-in — the controller always answers with the same generic message, so the
     * endpoint cannot be used to probe accounts.
     */
    @Transactional
    public void requestOtp(String email, boolean partnerDoor) {
        log.error("REQUEST RECEIVED");
        try{
            var found = userRepository.findByEmail(email.toLowerCase().trim());
            log.error("OTP lookup for '{}': present={}, active={}, role={}, partnerDoor={}", email,
                    found.isPresent(), found.map(User::isActive).orElse(null),
                    found.map(User::getRole).orElse(null), partnerDoor);
            found
                    .filter(User::isActive)
                    .filter(u -> belongsAt(u, partnerDoor))
                    .ifPresent(user -> {
                        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
                        user.setOtpCode(code);
                        user.setOtpExpiresAt(Instant.now().plus(OTP_TTL));
                        user.setOtpAttempts(0);
                        userRepository.save(user);
                        mailService.loginOtp(user, code);
                    });
        }catch (Exception e){
            log.error(e.getMessage(),e);
        }
    }

    /**
     * Single use: the code is cleared on success, and burned after 5 wrong guesses.
     * Wrong, expired or burned code → one generic 400.
     */
    @Transactional
    public AuthResponse verifyOtp(String email, String code, boolean partnerDoor) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .filter(User::isActive)
                .filter(u -> belongsAt(u, partnerDoor))
                .filter(u -> u.getOtpCode() != null
                        && u.getOtpExpiresAt() != null && u.getOtpExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new BadRequestException("Invalid or expired code"));
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
        userRepository.save(user);
        String title = staffProfileRepository.findByUser(user)
                .map(com.BeSpoke.entity.StaffProfile::getTitle).orElse(null);
        return new AuthResponse(jwtService.generateToken(user), UserDto.from(user, title), null);
    }
}
