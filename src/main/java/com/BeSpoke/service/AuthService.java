package com.BeSpoke.service;

import com.BeSpoke.dto.AuthResponse;
import com.BeSpoke.dto.LoginRequest;
import com.BeSpoke.dto.RegisterRequest;
import com.BeSpoke.dto.UserDto;
import com.BeSpoke.entity.ActivityType;
import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.LeadActivity;
import com.BeSpoke.entity.LeadSource;
import com.BeSpoke.entity.LeadStatus;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.repository.LeadActivityRepository;
import com.BeSpoke.repository.LeadRepository;
import com.BeSpoke.repository.UserRepository;
import com.BeSpoke.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final LeadRepository leadRepository;
    private final LeadActivityRepository leadActivityRepository;
    private final ScoreService scoreService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       LeadRepository leadRepository,
                       LeadActivityRepository leadActivityRepository,
                       ScoreService scoreService,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.leadRepository = leadRepository;
        this.leadActivityRepository = leadActivityRepository;
        this.scoreService = scoreService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Signing up IS the lead: User(CUSTOMER) + Lead(NEW_INQUIRY, WEBSITE) in one transaction. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("An account with this email already exists");
        }
        User user = new User(request.name().trim(), email,
                passwordEncoder.encode(request.password()), Role.CUSTOMER);
        user.setPhone(request.phone().trim());
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
        scoreService.rescore(lead, null);
        lead = leadRepository.save(lead);

        leadActivityRepository.save(new LeadActivity(lead, null, ActivityType.SYSTEM,
                "Lead created from website signup"));

        return new AuthResponse(jwtService.generateToken(user), UserDto.from(user), lead.getId());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        if (!user.isActive()) {
            throw new BadCredentialsException("Account is deactivated");
        }
        return new AuthResponse(jwtService.generateToken(user), UserDto.from(user), null);
    }
}
