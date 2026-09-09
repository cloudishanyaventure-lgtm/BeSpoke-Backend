package com.BeSpoke.controller;
import com.BeSpoke.dto.*;
import com.BeSpoke.service.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class RoomPlanController {
    private final RoomPlanService plans;
    private final CurrentUserService users;
    public RoomPlanController(RoomPlanService plans, CurrentUserService users) { this.plans = plans; this.users = users; }
    @GetMapping("/api/my/room-plans")
    public List<RoomPlanDto> mine(Authentication auth) { return plans.mine(users.requireByEmail(auth.getName())); }
    @PostMapping("/api/my/room-plans")
    public ResponseEntity<RoomPlanDto> create(Authentication auth, @Valid @RequestBody RoomPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(plans.save(users.requireByEmail(auth.getName()), null, request));
    }
    @PutMapping("/api/my/room-plans/{id}")
    public RoomPlanDto update(Authentication auth, @PathVariable Long id, @Valid @RequestBody RoomPlanRequest request) {
        return plans.save(users.requireByEmail(auth.getName()), id, request);
    }
    @GetMapping("/api/leads/{leadId}/room-plans")
    public List<RoomPlanDto> forLead(Authentication auth, @PathVariable Long leadId) {
        return plans.forLead(users.requireByEmail(auth.getName()), leadId);
    }
}
