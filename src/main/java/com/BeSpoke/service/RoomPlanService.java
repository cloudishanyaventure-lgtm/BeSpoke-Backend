package com.BeSpoke.service;
import com.BeSpoke.dto.*;
import com.BeSpoke.entity.*;
import com.BeSpoke.exception.*;
import com.BeSpoke.repository.RoomPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class RoomPlanService {
    private final RoomPlanRepository plans;
    private final RequirementService requirements;
    private final LeadService leads;
    public RoomPlanService(RoomPlanRepository plans, RequirementService requirements, LeadService leads) {
        this.plans = plans; this.requirements = requirements; this.leads = leads;
    }
    private void customer(User user) {
        if (user.getRole() != Role.CUSTOMER) throw new ForbiddenException("Customer account required");
    }
    public List<RoomPlanDto> mine(User user) {
        customer(user);
        return plans.findByOwnerOrderByUpdatedAtDesc(user).stream().map(RoomPlanDto::from).toList();
    }
    public List<RoomPlanDto> forLead(User actor, Long leadId) {
        if (!actor.getRole().worksLeads()) throw new ForbiddenException("Studio access required");
        Lead lead = leads.scopedLead(actor, leadId);
        return plans.findByLeadOrderByUpdatedAtDesc(lead).stream().map(RoomPlanDto::from).toList();
    }
    @Transactional
    public RoomPlanDto save(User user, Long id, RoomPlanRequest request) {
        customer(user);
        RoomPlan plan;
        if (id == null) {
            plan = new RoomPlan(); plan.setOwner(user); plan.setLead(requirements.myLead(user));
        } else {
            plan = plans.findByIdAndOwner(id, user).orElseThrow(() -> new NotFoundException("Room plan not found"));
            if (request.version() == null || !Objects.equals(plan.getVersion(), request.version()))
                throw new ConflictException("This room was updated on another device. Reopen the saved version or save your changes as a new room.");
        }
        if (request.width() == null || request.depth() == null || !Double.isFinite(request.width()) || !Double.isFinite(request.depth()))
            throw new BadRequestException("Room dimensions must be finite numbers");
        plan.setName(request.name().trim()); plan.setWall(request.wall()); plan.setFloor(request.floor());
        plan.setFabric(request.fabric()); plan.setWidth(request.width()); plan.setDepth(request.depth());
        plan.setLayout(request.layout()); plan.setRug(request.rug()); plan.setUpdatedAt(Instant.now());
        // Flush before returning so the client receives the incremented optimistic-lock version.
        return RoomPlanDto.from(plans.saveAndFlush(plan));
    }
}
