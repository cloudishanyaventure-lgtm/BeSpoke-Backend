package com.BeSpoke.service;

import com.BeSpoke.dto.MeDto;
import com.BeSpoke.dto.UpdateDesignerProfileRequest;
import com.BeSpoke.dto.UpdateMeRequest;
import com.BeSpoke.entity.DesignerProfile;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.exception.ForbiddenException;
import com.BeSpoke.repository.DesignerProfileRepository;
import com.BeSpoke.repository.ReviewRepository;
import com.BeSpoke.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Self-service profile management for the authenticated user. */
@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final DesignerProfileRepository designerProfileRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(UserRepository userRepository,
                          DesignerProfileRepository designerProfileRepository,
                          ReviewRepository reviewRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.designerProfileRepository = designerProfileRepository;
        this.reviewRepository = reviewRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public MeDto me(User user) {
        return toMeDto(user);
    }

    @Transactional
    public MeDto updateMe(User user, UpdateMeRequest request) {
        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new BadRequestException("Name must not be blank");
            }
            user.setName(request.name().trim());
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
        return toMeDto(userRepository.save(user));
    }

    @Transactional
    public MeDto updateMyDesignerProfile(User user, UpdateDesignerProfileRequest request) {
        if (user.getRole() != Role.DESIGNER) {
            throw new ForbiddenException("Only designers have a designer profile");
        }
        applyDesignerProfile(user, request);
        return toMeDto(user);
    }

    /** Applies the (partial) update to the designer's profile, creating it if missing. */
    @Transactional
    public DesignerProfile applyDesignerProfile(User designerUser, UpdateDesignerProfileRequest request) {
        DesignerProfile profile = designerProfileRepository.findByUserId(designerUser.getId())
                .orElseGet(() -> {
                    DesignerProfile created = new DesignerProfile();
                    created.setUser(designerUser);
                    return created;
                });
        if (request.bio() != null) {
            profile.setBio(request.bio());
        }
        if (request.specialties() != null) {
            profile.setSpecialties(request.specialties());
        }
        if (request.city() != null) {
            profile.setCity(request.city());
        }
        if (request.startingPrice() != null) {
            profile.setStartingPrice(request.startingPrice());
        }
        if (request.portfolioImageUrls() != null) {
            profile.getPortfolioImageUrls().clear();
            profile.getPortfolioImageUrls().addAll(request.portfolioImageUrls());
        }
        return designerProfileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public MeDto toMeDto(User user) {
        DesignerProfile profile = user.getRole() == Role.DESIGNER
                ? designerProfileRepository.findByUserId(user.getId()).orElse(null)
                : null;
        long reviewCount = profile != null ? reviewRepository.countByDesignerId(user.getId()) : 0L;
        return MeDto.from(user, profile, reviewCount);
    }
}
