package com.BeSpoke.controller;

import com.BeSpoke.dto.UpdateMeRequest;
import com.BeSpoke.dto.UserDto;
import com.BeSpoke.entity.StaffProfile;
import com.BeSpoke.entity.User;
import com.BeSpoke.repository.StaffProfileRepository;
import com.BeSpoke.repository.UserRepository;
import com.BeSpoke.service.CurrentUserService;
import com.BeSpoke.service.PublicProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final PublicProfileService publicProfileService;

    public MeController(CurrentUserService currentUserService,
                        UserRepository userRepository,
                        StaffProfileRepository staffProfileRepository,
                        PublicProfileService publicProfileService) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.publicProfileService = publicProfileService;
    }

    /** The staff profile, or null for customers who have none. */
    private StaffProfile profileOf(User user) {
        return staffProfileRepository.findByUser(user).orElse(null);
    }

    /** Only staff have a public designer card, so only they get a completion checklist. */
    private UserDto view(User user, StaffProfile profile) {
        List<String> missing = user.getRole().isStaff()
                ? publicProfileService.missingDesignerFields(user, profile) : null;
        return UserDto.from(user, profile, missing);
    }

    @GetMapping
    public UserDto me(Authentication authentication) {
        User user = currentUserService.requireByEmail(authentication.getName());
        return view(user, profileOf(user));
    }

    @PutMapping
    @Transactional
    public UserDto updateMe(Authentication authentication, @Valid @RequestBody UpdateMeRequest request) {
        User user = currentUserService.requireByEmail(authentication.getName());
        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
        }
        if (request.phone() != null && !request.phone().isBlank()) {
            user.setPhone(request.phone().trim());
        }
        if (request.city() != null && !request.city().isBlank()) {
            user.setCity(request.city().trim());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl().isBlank() ? null : request.avatarUrl().trim());
        }
        user = userRepository.save(user);

        StaffProfile profile = profileOf(user);
        if (profile != null) {
            if (request.bio() != null) {
                profile.setBio(request.bio().isBlank() ? null : request.bio().trim());
            }
            if (request.yearsExperience() != null) {
                profile.setYearsExperience(request.yearsExperience());
            }
            if (request.styles() != null) {
                // Mutable list — Hibernate cannot adopt an immutable one into a mapped collection.
                profile.setStyles(request.styles().stream()
                        .filter(s -> s != null && !s.isBlank()).map(String::trim).distinct()
                        .collect(Collectors.toCollection(ArrayList::new)));
            }
            profile = staffProfileRepository.save(profile);
        }
        return view(user, profile);
    }
}
