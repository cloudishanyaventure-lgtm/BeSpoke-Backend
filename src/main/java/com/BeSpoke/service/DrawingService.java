package com.BeSpoke.service;

import com.BeSpoke.dto.CreateDrawingRequest;
import com.BeSpoke.dto.DrawingDto;
import com.BeSpoke.dto.PendingApprovalDto;
import com.BeSpoke.dto.PrdSpaceDto;
import com.BeSpoke.entity.ActivityType;
import com.BeSpoke.entity.Drawing;
import com.BeSpoke.entity.DrawingStatus;
import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.LeadActivity;
import com.BeSpoke.entity.RequirementForm;
import com.BeSpoke.entity.RequirementRoom;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.exception.ConflictException;
import com.BeSpoke.exception.ForbiddenException;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.DrawingRepository;
import com.BeSpoke.repository.LeadActivityRepository;
import com.BeSpoke.repository.RequirementFormRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Drawing approval chain: WIP → PENDING_APPROVAL → APPROVED → FINAL, reject
 * back to WIP. Submitters who can approve drawings self-approve (the bypass
 * when a studio has junior roles disabled). Role gates live on the controller;
 * lead scoping (canSee, 404) here.
 */
@Service
@Transactional(readOnly = true)
public class DrawingService {

    private static final Logger log = LoggerFactory.getLogger(DrawingService.class);

    private final DrawingRepository drawingRepository;
    private final LeadActivityRepository leadActivityRepository;
    private final RequirementFormRepository requirementFormRepository;
    private final LeadService leadService;
    private final RequirementService requirementService;
    private final AuditService auditService;
    private final MailService mailService;

    public DrawingService(DrawingRepository drawingRepository,
                          LeadActivityRepository leadActivityRepository,
                          RequirementFormRepository requirementFormRepository,
                          LeadService leadService,
                          RequirementService requirementService,
                          AuditService auditService,
                          MailService mailService) {
        this.drawingRepository = drawingRepository;
        this.leadActivityRepository = leadActivityRepository;
        this.requirementFormRepository = requirementFormRepository;
        this.leadService = leadService;
        this.requirementService = requirementService;
        this.auditService = auditService;
        this.mailService = mailService;
    }

    public List<DrawingDto> list(User staff, Long leadId) {
        Lead lead = leadService.scopedLead(staff, leadId);
        return drawingRepository.findByLeadOrderByCreatedAtDesc(lead)
                .stream().map(DrawingDto::from).toList();
    }

    /** Customer view: every drawing of my lead, all statuses — the live pipeline. */
    public List<DrawingDto> myDrawings(User customer) {
        Lead lead = requirementService.myLead(customer);
        return drawingRepository.findByLeadOrderByCreatedAtDesc(lead)
                .stream().map(DrawingDto::from).toList();
    }

    /** The lead's PRD rooms — the picker the upload form offers instead of free text. */
    public List<PrdSpaceDto> prdSpaces(User staff, Long leadId) {
        Lead lead = leadService.scopedLead(staff, leadId);
        return requirementFormRepository.findByLead(lead)
                .map(form -> form.getRooms().stream().map(PrdSpaceDto::from).toList())
                .orElse(List.of());
    }

    @Transactional
    public DrawingDto create(User actor, Long leadId, CreateDrawingRequest request) {
        Lead lead = leadService.scopedLead(actor, leadId);
        String floor = trimToNull(request.floorLabel());
        String space = trimToNull(request.spaceLabel());
        if (floor == null || space == null) {
            throw new BadRequestException("Floor and space are required on every drawing");
        }
        String title = trimToNull(request.title());
        if (request.requirementRoomId() != null) {
            RequirementRoom room = prdRoom(lead, request.requirementRoomId());
            if (title == null) {
                title = derivedTitle(lead, room);
            }
        } else if (title == null) {
            throw new BadRequestException("Title is required, or pick a space from the brief");
        }
        Drawing drawing = new Drawing(lead, title, request.fileUrl(), actor.getName());
        drawing.setFloorLabel(floor);
        drawing.setSpaceLabel(space);
        drawing.setNotes(request.notes());
        drawing.setRequirementRoomId(request.requirementRoomId());
        return DrawingDto.from(drawingRepository.save(drawing));
    }

    /**
     * Drawings sitting at PENDING_APPROVAL and routed to *this* actor's role by the
     * enabled-roles chain. Non-approvers get an empty list, not an error — the studio
     * page asks for this section unconditionally.
     */
    public List<PendingApprovalDto> pending(User actor) {
        Role role = actor.getRole();
        List<Drawing> drawings;
        if (role.isPlatform()) {
            // The platform oversees every studio's queue.
            drawings = drawingRepository.findByStatusOrderBySubmittedAtAsc(DrawingStatus.PENDING_APPROVAL);
        } else if (role.canApproveDrawings() && actor.getCompany() != null) {
            drawings = drawingRepository.findByStatusAndLead_CompanyOrderBySubmittedAtAsc(
                            DrawingStatus.PENDING_APPROVAL, actor.getCompany()).stream()
                    .filter(drawing -> role.name().equals(DrawingDto.pendingWith(drawing)))
                    .toList();
        } else {
            return List.of();
        }
        return drawings.stream()
                .map(drawing -> new PendingApprovalDto(
                        drawing.getId(),
                        drawing.getLead().getId(),
                        drawing.getLead().getContactName(),
                        drawing.getTitle(),
                        drawing.getFloorLabel(),
                        drawing.getSpaceLabel(),
                        drawing.getUploadedByName(),
                        drawing.getSubmittedAt()))
                .toList();
    }

    @Transactional
    public DrawingDto submit(User actor, Long drawingId) {
        Drawing drawing = scopedDrawing(actor, drawingId);
        if (drawing.getStatus() != DrawingStatus.WIP) {
            throw new BadRequestException("Only WIP drawings can be submitted");
        }
        drawing.setSubmittedAt(Instant.now());
        drawing.setRejectionReason(null);
        if (actor.getRole().canApproveDrawings()) {
            // Self-approve: the submitter is an approver, no second pair of eyes needed.
            doApprove(drawing, actor);
        } else {
            drawing.setStatus(DrawingStatus.PENDING_APPROVAL);
        }
        return DrawingDto.from(drawingRepository.save(drawing));
    }

    @Transactional
    public DrawingDto approve(User actor, Long drawingId) {
        requireApprover(actor);
        Drawing drawing = scopedDrawing(actor, drawingId);
        if (drawing.getStatus() != DrawingStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Only drawings pending approval can be approved");
        }
        doApprove(drawing, actor);
        return DrawingDto.from(drawingRepository.save(drawing));
    }

    @Transactional
    public DrawingDto reject(User actor, Long drawingId, String reason) {
        requireApprover(actor);
        Drawing drawing = scopedDrawing(actor, drawingId);
        if (drawing.getStatus() != DrawingStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Only drawings pending approval can be rejected");
        }
        drawing.setStatus(DrawingStatus.WIP);
        drawing.setRejectionReason(reason.trim());
        leadActivityRepository.save(new LeadActivity(drawing.getLead(), actor, ActivityType.SYSTEM,
                "Drawing \"" + drawing.getTitle() + "\" rejected by " + actor.getName()));
        auditService.log(actor, drawing.getLead().getCompany(), "DRAWING_REJECTED",
                "Drawing \"" + drawing.getTitle() + "\" on lead #" + drawing.getLead().getId()
                        + " rejected: " + reason.trim());
        return DrawingDto.from(drawingRepository.save(drawing));
    }

    @Transactional
    public DrawingDto finalize(User actor, Long drawingId) {
        requireApprover(actor);
        Drawing drawing = scopedDrawing(actor, drawingId);
        if (drawing.getStatus() != DrawingStatus.APPROVED) {
            throw new BadRequestException("Only approved drawings can be finalized");
        }
        drawing.setStatus(DrawingStatus.FINAL);
        return DrawingDto.from(drawingRepository.save(drawing));
    }

    /** Customer sign-off: an APPROVED ("sent to customer") drawing becomes FINAL. */
    @Transactional
    public DrawingDto customerApprove(User customer, Long drawingId) {
        Drawing drawing = customerDrawing(customer, drawingId);
        if (drawing.getStatus() != DrawingStatus.APPROVED) {
            throw new ConflictException("Only drawings sent to you for approval can be approved");
        }
        drawing.setStatus(DrawingStatus.FINAL);
        drawing.setCustomerApprovedAt(Instant.now());
        leadActivityRepository.save(new LeadActivity(drawing.getLead(), customer, ActivityType.SYSTEM,
                "Drawing \"" + drawing.getTitle() + "\" approved by the customer"));
        return DrawingDto.from(drawingRepository.save(drawing));
    }

    /** Customer sends an APPROVED drawing back to WIP with the reason on it. */
    @Transactional
    public DrawingDto customerRequestChanges(User customer, Long drawingId, String reason) {
        Drawing drawing = customerDrawing(customer, drawingId);
        if (drawing.getStatus() != DrawingStatus.APPROVED) {
            throw new ConflictException("Changes can only be requested on drawings sent to you for approval");
        }
        drawing.setStatus(DrawingStatus.WIP);
        drawing.setRejectionReason(reason.trim());
        leadActivityRepository.save(new LeadActivity(drawing.getLead(), customer, ActivityType.SYSTEM,
                "Customer requested changes on \"" + drawing.getTitle() + "\": " + reason.trim()));
        return DrawingDto.from(drawingRepository.save(drawing));
    }

    /** 404 hides drawings that are not on the customer's own lead. */
    private Drawing customerDrawing(User customer, Long drawingId) {
        Lead lead = requirementService.myLead(customer);
        Drawing drawing = drawingRepository.findById(drawingId)
                .orElseThrow(() -> new NotFoundException("Drawing not found"));
        if (!drawing.getLead().getId().equals(lead.getId())) {
            throw new NotFoundException("Drawing not found");
        }
        return drawing;
    }

    private void doApprove(Drawing drawing, User actor) {
        drawing.setStatus(DrawingStatus.APPROVED);
        drawing.setApprovedByName(actor.getName());
        drawing.setApprovedAt(Instant.now());
        leadActivityRepository.save(new LeadActivity(drawing.getLead(), actor, ActivityType.SYSTEM,
                "Drawing \"" + drawing.getTitle() + "\" approved by " + actor.getName()));
        auditService.log(actor, drawing.getLead().getCompany(), "DRAWING_APPROVED",
                "Drawing \"" + drawing.getTitle() + "\" on lead #" + drawing.getLead().getId() + " approved");
        notifyCustomer(drawing);
    }

    /** "A new design is ready to view". Walk-in leads have no customer; mail never breaks approval. */
    private void notifyCustomer(Drawing drawing) {
        User customer = drawing.getLead().getCustomer();
        if (customer == null) {
            return;
        }
        try {
            mailService.drawingApproved(customer, drawing.getTitle());
        } catch (Exception ex) {
            log.warn("Could not email drawing approval for drawing #{}: {}", drawing.getId(), ex.getMessage());
        }
    }

    /**
     * Re-check the approver role against the database, not the JWT claim: a demoted
     * designer's 24h-old token still carries DESIGN_MANAGER past the @PreAuthorize gate.
     */
    private void requireApprover(User actor) {
        if (!actor.getRole().canApproveDrawings() && !actor.getRole().isPlatform()) {
            throw new ForbiddenException("Your role cannot approve drawings");
        }
    }

    /** The PRD room must belong to this lead's own brief — 400 otherwise. */
    private RequirementRoom prdRoom(Lead lead, Long roomId) {
        RequirementForm form = requirementFormRepository.findByLead(lead)
                .orElseThrow(() -> new BadRequestException("This lead has no brief yet"));
        return form.getRooms().stream()
                .filter(room -> roomId.equals(room.getId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("That space is not on this lead's brief"));
    }

    /** "Kitchen — 1st Floor", or "Kitchen" with no floor, plus " (v2)" per existing revision. */
    private String derivedTitle(Lead lead, RequirementRoom room) {
        String floor = trimToNull(room.getFloor());
        String title = floor == null ? room.getLabel() : room.getLabel() + " — " + floor;
        long existing = drawingRepository.countByLeadAndRequirementRoomId(lead, room.getId());
        return existing == 0 ? title : title + " (v" + (existing + 1) + ")";
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 404 hides drawings of leads outside the actor's scope (cross-tenant convention). */
    private Drawing scopedDrawing(User actor, Long drawingId) {
        Drawing drawing = drawingRepository.findById(drawingId)
                .orElseThrow(() -> new NotFoundException("Drawing not found"));
        if (!leadService.canSee(actor, drawing.getLead())) {
            throw new NotFoundException("Drawing not found");
        }
        return drawing;
    }
}
