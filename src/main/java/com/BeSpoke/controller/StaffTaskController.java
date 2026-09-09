package com.BeSpoke.controller;

import com.BeSpoke.dto.CreateStaffTaskRequest;
import com.BeSpoke.dto.StaffTaskDto;
import com.BeSpoke.service.CurrentUserService;
import com.BeSpoke.service.StaffTaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Tasks a company hands around internally. Company-scoped in the service. */
@RestController
@RequestMapping("/api/tasks")
public class StaffTaskController {

    private final StaffTaskService taskService;
    private final CurrentUserService currentUserService;

    public StaffTaskController(StaffTaskService taskService,
                               CurrentUserService currentUserService) {
        this.taskService = taskService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<StaffTaskDto> list(Authentication authentication) {
        return taskService.list(currentUserService.requireByEmail(authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<StaffTaskDto> create(Authentication authentication,
                                               @Valid @RequestBody CreateStaffTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(
                currentUserService.requireByEmail(authentication.getName()), request));
    }

    @GetMapping("/people")
    public List<StaffTaskService.Person> people(Authentication auth) {
        return taskService.people(currentUserService.requireByEmail(auth.getName()));
    }
    @GetMapping("/{id}")
    public StaffTaskDto get(Authentication auth, @PathVariable Long id) {
        return taskService.get(currentUserService.requireByEmail(auth.getName()), id);
    }
    @GetMapping("/{id}/comments")
    public List<com.BeSpoke.dto.TaskCommentDto> comments(Authentication auth, @PathVariable Long id) {
        return taskService.comments(currentUserService.requireByEmail(auth.getName()), id);
    }
    @PostMapping("/{id}/comments")
    public ResponseEntity<com.BeSpoke.dto.TaskCommentDto> comment(Authentication auth, @PathVariable Long id,
            @Valid @RequestBody com.BeSpoke.dto.TaskCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.comment(currentUserService.requireByEmail(auth.getName()), id, request));
    }

    @PutMapping("/{id}/status")
    public StaffTaskDto setStatus(Authentication authentication,
                                  @PathVariable Long id,
                                  @RequestParam String status) {
        return taskService.setStatus(
                currentUserService.requireByEmail(authentication.getName()), id, status);
    }
}
