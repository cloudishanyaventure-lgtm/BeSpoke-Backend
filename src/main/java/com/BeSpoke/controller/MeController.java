package com.BeSpoke.controller;

import com.BeSpoke.dto.UpdateMeRequest;
import com.BeSpoke.dto.UserDto;
import com.BeSpoke.entity.StaffProfile;
import com.BeSpoke.entity.User;
import com.BeSpoke.repository.StaffProfileRepository;
import com.BeSpoke.repository.UserRepository;
import com.BeSpoke.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final StaffProfileRepository staffProfileRepository;

    public MeController(CurrentUserService currentUserService,
                        UserRepository userRepository,
                        StaffProfileRepository staffProfileRepository) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.staffProfileRepository = staffProfileRepository;
    }

    /** The staff-profile designation, or null for customers who have no profile. */
    private String titleOf(User user) {
        return staffProfileRepository.findByUser(user).map(StaffProfile::getTitle).orElse(null);
    }

    @GetMapping
    public UserDto me(Authentication authentication) {
        User user = currentUserService.requireByEmail(authentication.getName());
        return UserDto.from(user, titleOf(user));
    }

    @PutMapping
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
        return UserDto.from(userRepository.save(user), titleOf(user));
    }
}
