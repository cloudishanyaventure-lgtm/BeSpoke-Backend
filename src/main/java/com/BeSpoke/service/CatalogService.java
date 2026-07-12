package com.BeSpoke.service;

import com.BeSpoke.dto.DesignerDto;
import com.BeSpoke.dto.ServiceDto;
import com.BeSpoke.entity.ServiceCategory;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.DesignServiceRepository;
import com.BeSpoke.repository.DesignerProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogService {

    private final DesignServiceRepository designServiceRepository;
    private final DesignerProfileRepository designerProfileRepository;

    public CatalogService(DesignServiceRepository designServiceRepository,
                          DesignerProfileRepository designerProfileRepository) {
        this.designServiceRepository = designServiceRepository;
        this.designerProfileRepository = designerProfileRepository;
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
        return designerProfileRepository.findAll().stream().map(DesignerDto::from).toList();
    }

    @Transactional(readOnly = true)
    public DesignerDto getDesigner(Long id) {
        return designerProfileRepository.findById(id)
                .map(DesignerDto::from)
                .orElseThrow(() -> new NotFoundException("Designer not found: " + id));
    }
}
