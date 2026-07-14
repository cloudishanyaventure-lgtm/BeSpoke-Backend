package com.BeSpoke.controller;

import com.BeSpoke.dto.AdminCreateUserRequest;
import com.BeSpoke.dto.AdminUpdateUserRequest;
import com.BeSpoke.dto.AdminUserDto;
import com.BeSpoke.dto.DesignerDto;
import com.BeSpoke.dto.UpdateDesignerProfileRequest;
import com.BeSpoke.entity.User;
import com.BeSpoke.service.AdminUserService;
import com.BeSpoke.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final CurrentUserService currentUserService;

    public AdminUserController(AdminUserService adminUserService,
                               CurrentUserService currentUserService) {
        this.adminUserService = adminUserService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<AdminUserDto> list(@RequestParam(required = false) String role,
                                   @RequestParam(required = false) String q) {
        return adminUserService.listUsers(role, q);
    }

    @PostMapping
    public ResponseEntity<AdminUserDto> create(@Valid @RequestBody AdminCreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUserService.createUser(request));
    }

    @PutMapping("/{id}")
    public AdminUserDto update(@PathVariable Long id,
                               @Valid @RequestBody AdminUpdateUserRequest request) {
        return adminUserService.updateUser(id, request);
    }

    /** Soft delete: sets active=false. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(Authentication authentication, @PathVariable Long id) {
        User admin = currentUserService.requireByEmail(authentication.getName());
        adminUserService.deactivateUser(admin, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    public AdminUserDto activate(@PathVariable Long id) {
        return adminUserService.activateUser(id);
    }

    @PutMapping("/{id}/designer-profile")
    public DesignerDto updateDesignerProfile(@PathVariable Long id,
                                             @Valid @RequestBody UpdateDesignerProfileRequest request) {
        return adminUserService.updateDesignerProfile(id, request);
    }
}
