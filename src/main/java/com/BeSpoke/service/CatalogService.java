package com.BeSpoke.service;

import com.BeSpoke.dto.DesignerDto;
import com.BeSpoke.dto.ServiceDto;
import com.BeSpoke.entity.DesignerProfile;
import com.BeSpoke.entity.ServiceCategory;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.DesignServiceRepository;
import com.BeSpoke.repository.DesignerProfileRepository;
import com.BeSpoke.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogService {

    private final DesignServiceRepository designServiceRepository;
    private final DesignerProfileRepository designerProfileRepository;
    private final ReviewRepository reviewRepository;

    public CatalogService(DesignServiceRepository designServiceRepository,
                          DesignerProfileRepository designerProfileRepository,
                          ReviewRepository reviewRepository) {
        this.designServiceRepository = designServiceRepository;
        this.designerProfileRepository = designerProfileRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional(readOnly = true)
    public List<ServiceDto> listServices(String category) {
        if (category == null || category.isBlank()) {
            return designServiceRepository.findAll().stream().map(ServiceDto::from).toList();
        }
        ServiceCategory cat;
        try {
            cat = ServiceCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unknown category: " + category);
        }
        return designServiceRepository.findByCategory(cat).stream().map(ServiceDto::from).toList();
    }

    @Transactional(readOnly = true)
    public ServiceDto getService(Long id) {
        return designServiceRepository.findById(id)
                .map(ServiceDto::from)
                .orElseThrow(() -> new NotFoundException("Service not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<DesignerDto> listDesigners() {
        return designerProfileRepository.findAll().stream()
                .filter(profile -> profile.getUser().isActive())
                .map(this::toDesignerDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public DesignerDto getDesigner(Long id) {
        // Resolve by USER id first (what the frontend, views and reviews all
        // key on), falling back to profile id for backward compatibility.
        DesignerProfile profile = designerProfileRepository.findByUserId(id)
                .or(() -> designerProfileRepository.findById(id))
                .filter(p -> p.getUser().isActive())
                .orElseThrow(() -> new NotFoundException("Designer not found: " + id));
        return toDesignerDto(profile);
    }

    /** Atomically bumps the profile view counter for a designer (by user id); returns the new count. */
    @Transactional
    public long incrementDesignerViews(Long userId) {
        int updated = designerProfileRepository.incrementViewCountByUserId(userId);
        if (updated == 0) {
            throw new NotFoundException("Designer not found: " + userId);
        }
        return designerProfileRepository.findByUserId(userId)
                .map(DesignerProfile::getViewCount)
                .orElseThrow(() -> new NotFoundException("Designer not found: " + userId));
    }

    private DesignerDto toDesignerDto(DesignerProfile profile) {
        Long userId = profile.getUser().getId();
        return DesignerDto.from(profile,
                reviewRepository.countByDesignerId(userId),
                reviewRepository.averageRatingForDesigner(userId));
    }
}
