package com.BeSpoke.controller;

import com.BeSpoke.dto.MeDto;
import com.BeSpoke.dto.UpdateDesignerProfileRequest;
import com.BeSpoke.dto.UpdateMeRequest;
import com.BeSpoke.entity.User;
import com.BeSpoke.service.CurrentUserService;
import com.BeSpoke.service.ProfileService;
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

    private final ProfileService profileService;
    private final CurrentUserService currentUserService;

    public MeController(ProfileService profileService, CurrentUserService currentUserService) {
        this.profileService = profileService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public MeDto me(Authentication authentication) {
        User user = currentUserService.requireByEmail(authentication.getName());
        return profileService.me(user);
    }

    @PutMapping
    public MeDto updateMe(Authentication authentication,
                          @Valid @RequestBody UpdateMeRequest request) {
        User user = currentUserService.requireByEmail(authentication.getName());
        return profileService.updateMe(user, request);
    }

    /** DESIGNER only (enforced in SecurityConfig). */
    @PutMapping("/designer-profile")
    public MeDto updateDesignerProfile(Authentication authentication,
                                       @Valid @RequestBody UpdateDesignerProfileRequest request) {
        User user = currentUserService.requireByEmail(authentication.getName());
        return profileService.updateMyDesignerProfile(user, request);
    }
}
