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
    private final AuditService auditService;
    private final MailService mailService;
    private final DocumentService documents;
    private final NotificationService notifications;
    private final com.BeSpoke.repository.UserRepository users;

    public DrawingService(DrawingRepository drawingRepository,
                          LeadActivityRepository leadActivityRepository,
                          RequirementFormRepository requirementFormRepository,
                          LeadService leadService,
                          AuditService auditService,
                          MailService mailService,
                          DocumentService documents,
                          NotificationService notifications,
                          com.BeSpoke.repository.UserRepository users) {
        this.drawingRepository = drawingRepository;
        this.leadActivityRepository = leadActivityRepository;
        this.requirementFormRepository = requirementFormRepository;
        this.leadService = leadService;
        this.auditService = auditService;
        this.mailService = mailService;
        this.documents = documents;
        this.notifications = notifications;
        this.users = users;
    }

    public List<DrawingDto> list(User staff, Long leadId) {
        Lead lead = leadService.scopedLead(staff, leadId);
        return drawingRepository.findByLeadOrderByCreatedAtDesc(lead)
                .stream().map(DrawingDto::from).toList();
    }

    /** Shared designs across all of the customer's leads; internal drafts stay private. */
    public List<DrawingDto> myDrawings(User customer) {
        requireCustomer(customer);
        return drawingRepository.findByLead_CustomerAndStatusInOrderByCreatedAtDesc(customer,
                        List.of(DrawingStatus.APPROVED, DrawingStatus.FINAL, DrawingStatus.CHANGES_REQUESTED))
                .stream().map(DrawingDto::forCustomer).toList();
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
        requireUploader(actor);
        Lead lead = leadService.scopedLead(actor, leadId);
        Drawing previous = null;
        if (request.previousRevisionId() != null) {
            previous = scopedDrawing(actor, request.previousRevisionId());
            if (!previous.getLead().getId().equals(leadId)) throw new NotFoundException("Drawing not found");
            requireLatest(previous);
            if (previous.getStatus() != DrawingStatus.CHANGES_REQUESTED && previous.getStatus() != DrawingStatus.FINAL
                    && !(previous.getStatus() == DrawingStatus.WIP && previous.getRejectionReason() != null)) {
                throw new ConflictException("Request changes or complete review before uploading a new version");
            }
        }
        String floor = trimToNull(request.floorLabel());
        String space = trimToNull(request.spaceLabel());
        if (floor == null || space == null) {
            throw new BadRequestException("Floor and space are required on every drawing");
        }
        String title = trimToNull(request.title());
        if (previous != null) {
            // Room IDs can change when the brief is edited; retain the original space association.
            title = previous.getTitle();
            floor = previous.getFloorLabel();
            space = previous.getSpaceLabel();
        } else if (request.requirementRoomId() != null) {
            RequirementRoom room = prdRoom(lead, request.requirementRoomId());
            if (title == null) {
                title = derivedTitle(room);
            }
        } else if (title == null) {
            throw new BadRequestException("Title is required, or pick a space from the brief");
        }
        Drawing drawing = new Drawing(lead, title, request.fileUrl(), actor.getName());
        drawing.setDocument(documents.designAsset(actor,leadId,request.fileUrl()));
        drawing.setFloorLabel(floor);
        drawing.setSpaceLabel(space);
        drawing.setNotes(request.notes());
        drawing.setRequirementRoomId(previous == null ? request.requirementRoomId() : previous.getRequirementRoomId());
        drawing.setUploadedBy(actor);
        if (previous != null) {
            drawing.setPreviousRevision(previous);
            drawing.setRevisionNumber(previous.getRevisionNumber() + 1);
            previous.setSupersededAt(Instant.now());
            // Flush the optimistic lock before inserting the unique successor.
            drawingRepository.saveAndFlush(previous);
        }
        drawingRepository.saveAndFlush(drawing);
        record(drawing, actor, "DRAWING_CREATED", "Uploaded " + label(drawing) + " as WIP");
        return DrawingDto.from(drawing);
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
        requireUploader(actor);
        Drawing drawing = scopedDrawing(actor, drawingId);
        requireLatest(drawing);
        if (drawing.getStatus() != DrawingStatus.WIP) {
            throw new BadRequestException("Only WIP drawings can be submitted");
        }
        if (drawing.getRejectionReason() != null) {
            throw new ConflictException("Upload a new version to address the review feedback");
        }
        drawing.setSubmittedAt(Instant.now());
        if (actor.getRole().canApproveDrawings() || actor.getRole().isPlatform()) {
            // Self-approve: the submitter is an approver, no second pair of eyes needed.
            doApprove(drawing, actor);
        } else {
            drawing.setStatus(DrawingStatus.PENDING_APPROVAL);
            record(drawing, actor, "DRAWING_SUBMITTED", label(drawing) + ": WIP → PENDING_APPROVAL");
            notifyReviewers(drawing);
        }
        return DrawingDto.from(drawingRepository.save(drawing));
    }

    @Transactional
    public DrawingDto approve(User actor, Long drawingId) {
        requireApprover(actor);
        Drawing drawing = scopedDrawing(actor, drawingId);
        requireLatest(drawing);
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
        requireLatest(drawing);
        if (drawing.getStatus() != DrawingStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Only drawings pending approval can be rejected");
        }
        reason = requireReason(reason);
        drawing.setStatus(DrawingStatus.WIP);
        drawing.setRejectionReason(reason);
        record(drawing, actor, "DRAWING_REJECTED", label(drawing) + ": PENDING_APPROVAL → WIP: " + reason);
        notifyStudio(drawing, actor, "Design needs revision", label(drawing) + ": " + reason);
        return DrawingDto.from(drawingRepository.save(drawing));
    }

    @Transactional
    public DrawingDto finalize(User actor, Long drawingId) {
        requireApprover(actor);
        Drawing drawing = scopedDrawing(actor, drawingId);
        requireLatest(drawing);
        if (drawing.getLead().getCustomer() != null) {
            throw new ForbiddenException("The customer must approve this design from their account");
        }
        if (drawing.getStatus() != DrawingStatus.APPROVED) {
            throw new ConflictException("Only approved drawings can be finalized");
        }
        drawing.setStatus(DrawingStatus.FINAL);
        drawing.setFinalizedBy(actor);
        drawing.setFinalizedAt(Instant.now());
        record(drawing, actor, "DRAWING_FINALIZED", label(drawing) + ": APPROVED → FINAL (walk-in sign-off)");
        notifyStudio(drawing, actor, "Design finalized", label(drawing) + " was finalized for a walk-in customer.");
        return DrawingDto.from(drawingRepository.save(drawing));
    }

    @Transactional
    public DrawingDto customerApprove(User customer, Long drawingId) {
        Drawing drawing = customerDrawing(customer, drawingId);
        requireLatest(drawing);
        // A retried successful decision must not duplicate its history or notifications.
        if (drawing.getStatus() == DrawingStatus.FINAL && drawing.getCustomerApprovedAt() != null) {
            return DrawingDto.forCustomer(drawing);
        }
        requireAwaitingCustomer(drawing);
        Instant now = Instant.now();
        drawing.setStatus(DrawingStatus.FINAL);
        drawing.setCustomerApprovedAt(now);
        drawing.setCustomerDecidedBy(customer);
        drawing.setCustomerDecidedAt(now);
        record(drawing, customer, "DRAWING_CUSTOMER_APPROVED", label(drawing) + ": APPROVED → FINAL by customer #" + customer.getId());
        notifyStudio(drawing, customer, "Customer approved a design", label(drawing) + " is approved by " + customer.getName() + ".");
        return DrawingDto.forCustomer(drawingRepository.save(drawing));
    }

    @Transactional
    public DrawingDto customerRequestChanges(User customer, Long drawingId, String reason) {
        reason = requireReason(reason);
        Drawing drawing = customerDrawing(customer, drawingId);
        requireLatest(drawing);
        if (drawing.getStatus() == DrawingStatus.CHANGES_REQUESTED && reason.equals(drawing.getRejectionReason())) {
            return DrawingDto.forCustomer(drawing);
        }
        requireAwaitingCustomer(drawing);
        drawing.setStatus(DrawingStatus.CHANGES_REQUESTED);
        drawing.setRejectionReason(reason);
        drawing.setCustomerDecidedBy(customer);
        drawing.setCustomerDecidedAt(Instant.now());
        record(drawing, customer, "DRAWING_CHANGES_REQUESTED", label(drawing) + ": APPROVED → CHANGES_REQUESTED: " + reason);
        notifyStudio(drawing, customer, "Customer requested design changes", label(drawing) + ": " + reason);
        return DrawingDto.forCustomer(drawingRepository.save(drawing));
    }

    private Drawing customerDrawing(User customer, Long drawingId) {
        requireCustomer(customer);
        Drawing drawing = drawingRepository.findById(drawingId)
                .orElseThrow(() -> new NotFoundException("Drawing not found"));
        User owner = drawing.getLead().getCustomer();
        if (owner == null || !owner.getId().equals(customer.getId()) || !drawing.isCustomerVisible()) {
            throw new NotFoundException("Drawing not found");
        }
        return drawing;
    }

    private static void requireCustomer(User user) {
        if (user.getRole() != Role.CUSTOMER) throw new ForbiddenException("Customer account required");
    }

    private static void requireAwaitingCustomer(Drawing drawing) {
        if (drawing.getStatus() != DrawingStatus.APPROVED) {
            throw new ConflictException("Only designs awaiting your approval can receive a decision");
        }
    }

    private static void requireLatest(Drawing drawing) {
        if (drawing.getSupersededAt() != null) throw new ConflictException("A newer version exists. Reload the designs before continuing.");
    }

    private static String requireReason(String reason) {
        if (reason == null || reason.isBlank() || reason.trim().length() > 500)
            throw new BadRequestException("Describe the changes in 1–500 characters");
        return reason.trim();
    }

    private static void requireUploader(User actor) {
        if (!(actor.getRole().isPlatform() || actor.getRole().canApproveDrawings()
                || actor.getRole() == Role.DESIGNER || actor.getRole() == Role.PROJECT_MANAGER))
            throw new ForbiddenException("Your role cannot upload drawings");
    }

    private static String label(Drawing drawing) {
        return "Design #" + drawing.getId() + " “" + drawing.getTitle() + "” V" + drawing.getRevisionNumber();
    }

    private void record(Drawing drawing, User actor, String action, String detail) {
        leadActivityRepository.save(new LeadActivity(drawing.getLead(), actor, ActivityType.SYSTEM, detail));
        auditService.log(actor, drawing.getLead().getCompany(), action,
                "Lead #" + drawing.getLead().getId() + ": " + detail);
    }

    private void notifyReviewers(Drawing drawing) {
        if (drawing.getLead().getCompany() == null) return;
        String role = DrawingDto.pendingWith(drawing);
        users.findByCompanyAndRole(drawing.getLead().getCompany(), Role.valueOf(role)).stream()
                .filter(User::isActive)
                .filter(user -> leadService.canSee(user, drawing.getLead()))
                .forEach(user -> notifications.publish(user, "Design ready for studio review", label(drawing),
                        "/studio/leads/" + drawing.getLead().getId()));
    }

    private void notifyStudio(Drawing drawing, User actor, String title, String body) {
        java.util.Map<Long, User> recipients = new java.util.LinkedHashMap<>();
        for (User user : new User[]{drawing.getUploadedBy(), drawing.getLead().getAssignedDesigner()}) {
            if (user != null) recipients.put(user.getId(), user);
        }
        // Directors receive unassigned/legacy drawings; do not broadcast customer feedback to the company.
        if (recipients.values().stream().noneMatch(user -> eligibleRecipient(user, drawing)) && drawing.getLead().getCompany() != null) {
            users.findByCompanyAndRole(drawing.getLead().getCompany(), Role.DIRECTOR)
                    .forEach(user -> recipients.put(user.getId(), user));
        }
        recipients.values().stream().filter(user -> !user.getId().equals(actor.getId()))
                .filter(user -> eligibleRecipient(user, drawing))
                .forEach(user -> notifications.publish(user, title, body, "/studio/leads/" + drawing.getLead().getId()));
    }

    private boolean eligibleRecipient(User user, Drawing drawing) {
        return user.isActive() && leadService.canSee(user, drawing.getLead())
                && (user.getRole().isPlatform() || (user.getCompany() != null && user.getCompany().isActive()
                && user.getCompany().effectiveEnabledRoles().contains(user.getRole())));
    }

    private void doApprove(Drawing drawing, User actor) {
        drawing.setStatus(DrawingStatus.APPROVED);
        drawing.setApprovedByName(actor.getName());
        drawing.setApprovedAt(Instant.now());
        record(drawing, actor, "DRAWING_APPROVED", label(drawing) + ": studio review → APPROVED by " + actor.getName());
        notifications.publish(drawing.getLead().getCustomer(), "A design is ready for your review",
                label(drawing) + " is ready. Approve it or request changes.", "/my/designs");
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

    /** "Kitchen — 1st Floor", or "Kitchen" with no floor, without a version suffix; revisionNumber is stored separately. */
    private String derivedTitle(RequirementRoom room) {
        String floor = trimToNull(room.getFloor());
        String title = floor == null ? room.getLabel() : room.getLabel() + " — " + floor;
        return title;
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
