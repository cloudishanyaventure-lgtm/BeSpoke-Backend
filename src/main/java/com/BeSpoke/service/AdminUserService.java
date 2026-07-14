package com.BeSpoke.service;

import com.BeSpoke.dto.AdminCreateUserRequest;
import com.BeSpoke.dto.AdminUpdateUserRequest;
import com.BeSpoke.dto.AdminUserDto;
import com.BeSpoke.dto.DesignerDto;
import com.BeSpoke.dto.UpdateDesignerProfileRequest;
import com.BeSpoke.entity.DesignerProfile;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.exception.ForbiddenException;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.DesignerProfileRepository;
import com.BeSpoke.repository.ReviewRepository;
import com.BeSpoke.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Admin-side user management: listing, CRUD, soft delete and designer profile edits. */
@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final DesignerProfileRepository designerProfileRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfileService profileService;

    public AdminUserService(UserRepository userRepository,
                            DesignerProfileRepository designerProfileRepository,
                            ReviewRepository reviewRepository,
                            PasswordEncoder passwordEncoder,
                            ProfileService profileService) {
        this.userRepository = userRepository;
        this.designerProfileRepository = designerProfileRepository;
        this.reviewRepository = reviewRepository;
        this.passwordEncoder = passwordEncoder;
        this.profileService = profileService;
    }

    @Transactional(readOnly = true)
    public List<AdminUserDto> listUsers(String role, String q) {
        Role roleFilter = null;
        if (role != null && !role.isBlank()) {
            try {
                roleFilter = Role.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Unknown role: " + role);
            }
        }
        String query = q == null ? null : q.trim().toLowerCase();
        Role finalRoleFilter = roleFilter;
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(user -> finalRoleFilter == null || user.getRole() == finalRoleFilter)
                .filter(user -> query == null || query.isEmpty()
                        || user.getName().toLowerCase().contains(query)
                        || user.getEmail().toLowerCase().contains(query))
                .map(AdminUserDto::from)
                .toList();
    }

    @Transactional
    public AdminUserDto createUser(AdminCreateUserRequest request) {
        String email = request.email().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("An account with this email already exists");
        }
        Role role = Role.valueOf(request.role()); // validated by the DTO pattern
        User user = new User(request.name().trim(), email,
                passwordEncoder.encode(request.password()), role);
        user = userRepository.save(user);

        if (role == Role.DESIGNER) {
            DesignerProfile profile = new DesignerProfile();
            profile.setUser(user);
            designerProfileRepository.save(profile);
        }
        return AdminUserDto.from(user);
    }

    @Transactional
    public AdminUserDto updateUser(Long id, AdminUpdateUserRequest request) {
        User user = requireUser(id);
        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new BadRequestException("Name must not be blank");
            }
            user.setName(request.name().trim());
        }
        if (request.email() != null) {
            String email = request.email().toLowerCase().trim();
            if (email.isBlank()) {
                throw new BadRequestException("Email must not be blank");
            }
            if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
                throw new BadRequestException("An account with this email already exists");
            }
            user.setEmail(email);
        }
        if (request.phone() != null) {
            user.setPhone(request.phone().trim());
        }
        if (request.city() != null) {
            user.setCity(request.city().trim());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl().trim());
        }
        if (request.password() != null) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return AdminUserDto.from(userRepository.save(user));
    }

    /** Soft delete: marks the user inactive. */
    @Transactional
    public void deactivateUser(User currentAdmin, Long id) {
        User user = requireUser(id);
        if (user.getId().equals(currentAdmin.getId())) {
            throw new ForbiddenException("You cannot deactivate your own account");
        }
        if (user.getRole() == Role.ADMIN && user.isActive()
                && userRepository.countByRoleAndActiveTrue(Role.ADMIN) <= 1) {
            throw new BadRequestException("Cannot deactivate the last active admin");
        }
        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public AdminUserDto activateUser(Long id) {
        User user = requireUser(id);
        user.setActive(true);
        return AdminUserDto.from(userRepository.save(user));
    }

    @Transactional
    public DesignerDto updateDesignerProfile(Long userId, UpdateDesignerProfileRequest request) {
        User user = requireUser(userId);
        if (user.getRole() != Role.DESIGNER) {
            throw new BadRequestException("User " + userId + " is not a designer");
        }
        DesignerProfile profile = profileService.applyDesignerProfile(user, request);
        return DesignerDto.from(profile,
                reviewRepository.countByDesignerId(userId),
                reviewRepository.averageRatingForDesigner(userId));
    }

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }
}
