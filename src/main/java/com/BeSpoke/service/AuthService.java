package com.BeSpoke.service;

import com.BeSpoke.dto.AuthResponse;
import com.BeSpoke.dto.LoginRequest;
import com.BeSpoke.dto.RegisterRequest;
import com.BeSpoke.entity.DesignerProfile;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.repository.DesignerProfileRepository;
import com.BeSpoke.repository.UserRepository;
import com.BeSpoke.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final DesignerProfileRepository designerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       DesignerProfileRepository designerProfileRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.designerProfileRepository = designerProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("An account with this email already exists");
        }
        Role role = Role.valueOf(request.role()); // validated as CUSTOMER|DESIGNER by the DTO
        User user = new User(request.name().trim(), email, passwordEncoder.encode(request.password()), role);
        user = userRepository.save(user);

        if (role == Role.DESIGNER) {
            DesignerProfile profile = new DesignerProfile();
            profile.setUser(user);
            designerProfileRepository.save(profile);
        }
        return toAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }
}
