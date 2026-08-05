package com.BeSpoke.service;

import com.BeSpoke.dto.RequirementFormDto;
import com.BeSpoke.dto.RequirementFormRequest;
import com.BeSpoke.dto.RoomRequest;
import com.BeSpoke.entity.ActivityType;
import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.LeadActivity;
import com.BeSpoke.entity.QuoteStatus;
import com.BeSpoke.entity.RequirementForm;
import com.BeSpoke.entity.RequirementFormStatus;
import com.BeSpoke.entity.RequirementRoom;
import com.BeSpoke.entity.RequirementRoomItem;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.exception.ConflictException;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.LeadActivityRepository;
import com.BeSpoke.repository.LeadRepository;
import com.BeSpoke.repository.QuoteRepository;
import com.BeSpoke.repository.RequirementFormRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RequirementService {

    private final LeadRepository leadRepository;
    private final RequirementFormRepository requirementFormRepository;
    private final QuoteRepository quoteRepository;
    private final LeadActivityRepository leadActivityRepository;
    private final ScoreService scoreService;
    private final MailService mailService;

    public RequirementService(LeadRepository leadRepository,
                              RequirementFormRepository requirementFormRepository,
                              QuoteRepository quoteRepository,
                              LeadActivityRepository leadActivityRepository,
                              ScoreService scoreService,
                              MailService mailService) {
        this.leadRepository = leadRepository;
        this.requirementFormRepository = requirementFormRepository;
        this.quoteRepository = quoteRepository;
        this.leadActivityRepository = leadActivityRepository;
        this.scoreService = scoreService;
        this.mailService = mailService;
    }

    public Lead myLead(User customer) {
        return leadRepository.findFirstByCustomerOrderByCreatedAtDesc(customer)
                .orElseThrow(() -> new NotFoundException("No lead found for your account"));
    }

    @Transactional(readOnly = true)
    public RequirementFormDto myForm(User customer) {
        RequirementForm form = requirementFormRepository.findByLead(myLead(customer))
                .orElseThrow(() -> new NotFoundException("Requirement form not started yet"));
        return RequirementFormDto.from(form);
    }

    @Transactional
    public RequirementFormDto upsert(User customer, RequirementFormRequest request) {
        Lead lead = myLead(customer);
        assertNotLocked(lead);
        RequirementForm form = requirementFormRepository.findByLead(lead).orElseGet(() -> {
            RequirementForm created = new RequirementForm();
            created.setLead(lead);
            created.setStatus(RequirementFormStatus.DRAFT);
            return created;
        });
        applyScalars(form, request);
        form.setUpdatedAt(Instant.now());
        form = requirementFormRepository.save(form);
        rescore(lead, form);
        return RequirementFormDto.from(form);
    }

    /** Replaces all rooms (and their selected items) wholesale. */
    @Transactional
    public RequirementFormDto replaceRooms(User customer, List<RoomRequest> roomRequests) {
        Lead lead = myLead(customer);
        assertNotLocked(lead);
        return applyRooms(lead, roomRequests);
    }

    /** Staff PRD edit — same wholesale replace, but skips the quote lock: staff own the PRD after handoff. */
    @Transactional
    public RequirementFormDto staffReplaceRooms(Lead lead, List<RoomRequest> roomRequests) {
        return applyRooms(lead, roomRequests);
    }

    /**
     * Staff filling in the brief the customer gave them — over a call, on site, anywhere.
     * It is the same document either way, so this writes the same fields the customer's
     * wizard writes and skips the quote lock like the rooms path does.
     */
    @Transactional
    public RequirementFormDto staffUpsertForm(Lead lead, RequirementFormRequest request) {
        RequirementForm form = requirementFormRepository.findByLead(lead).orElseGet(() -> {
            RequirementForm created = new RequirementForm();
            created.setLead(lead);
            created.setStatus(RequirementFormStatus.DRAFT);
            return created;
        });
        unfreezeIfApproved(lead, form);
        applyScalars(form, request);
        form.setUpdatedAt(Instant.now());
        form = requirementFormRepository.save(form);
        rescore(lead, form);
        return RequirementFormDto.from(form);
    }

    /**
     * Staff marking the brief complete on the customer's behalf. This is what stops the
     * customer's portal nagging them to "complete your profile" for information they have
     * already given — the studio captured it, so the brief is done.
     */
    @Transactional
    public RequirementFormDto staffSubmit(Lead lead, User staff) {
        RequirementForm form = requirementFormRepository.findByLead(lead)
                .orElseThrow(() -> new BadRequestException("Capture the brief before marking it complete"));
        if (form.getStatus() == RequirementFormStatus.SUBMITTED) {
            return RequirementFormDto.from(form);
        }
        form.setStatus(RequirementFormStatus.SUBMITTED);
        form.setApprovedAt(null);
        form.setSubmittedAt(Instant.now());
        form.setUpdatedAt(Instant.now());
        form = requirementFormRepository.save(form);
        leadActivityRepository.save(new LeadActivity(lead, staff, ActivityType.SYSTEM,
                "Requirements captured by " + staff.getName() + " on the customer's behalf"));
        rescore(lead, form);
        return RequirementFormDto.from(form);
    }

    private RequirementFormDto applyRooms(Lead lead, List<RoomRequest> roomRequests) {
        RequirementForm form = requirementFormRepository.findByLead(lead).orElseGet(() -> {
            RequirementForm created = new RequirementForm();
            created.setLead(lead);
            created.setStatus(RequirementFormStatus.DRAFT);
            return requirementFormRepository.save(created);
        });
        unfreezeIfApproved(lead, form);
        form.getRooms().clear();
        int order = 0;
        for (RoomRequest roomRequest : roomRequests) {
            RequirementRoom room = new RequirementRoom();
            room.setForm(form);
            room.setRoomType(roomRequest.roomType().trim());
            room.setLabel(roomRequest.label().trim());
            room.setFloor(roomRequest.floor());
            room.setFamilyMember(roomRequest.familyMember());
            room.setPrimaryUse(roomRequest.primaryUse());
            room.setMustHaves(roomRequest.mustHaves());
            room.setReuseFurniture(roomRequest.reuseFurniture());
            room.setStorageNeeds(roomRequest.storageNeeds());
            room.setColorPreference(roomRequest.colorPreference());
            room.setSpecialRequirements(roomRequest.specialRequirements());
            room.setNotes(roomRequest.notes());
            room.setSortOrder(order++);
            if (roomRequest.items() != null) {
                for (RoomRequest.RoomItemRequest itemRequest : roomRequest.items()) {
                    room.getItems().add(new RequirementRoomItem(
                            room, itemRequest.category().trim(), itemRequest.item().trim(), itemRequest.note()));
                }
            }
            form.getRooms().add(room);
        }
        form.setUpdatedAt(Instant.now());
        form = requirementFormRepository.save(form);
        rescore(lead, form);
        return RequirementFormDto.from(form);
    }

    @Transactional
    public RequirementFormDto submit(User customer) {
        Lead lead = myLead(customer);
        assertNotLocked(lead);
        RequirementForm form = requirementFormRepository.findByLead(lead)
                .orElseThrow(() -> new BadRequestException("Fill in the requirement form before submitting"));
        if (form.getStatus() == RequirementFormStatus.SUBMITTED) {
            throw new BadRequestException("Requirement form is already submitted");
        }
        form.setStatus(RequirementFormStatus.SUBMITTED);
        form.setSubmittedAt(Instant.now());
        form.setUpdatedAt(Instant.now());
        form = requirementFormRepository.save(form);
        leadActivityRepository.save(new LeadActivity(lead, customer, ActivityType.SYSTEM,
                "Requirements captured — customer submitted the requirement form"));
        rescore(lead, form);
        // MailService swallows failures; the submit never breaks on a mail error.
        mailService.briefSubmitted(customer);
        return RequirementFormDto.from(form);
    }

    /** Customer sign-off on the submitted brief — locks it for further customer edits. */
    @Transactional
    public RequirementFormDto approve(User customer) {
        Lead lead = myLead(customer);
        RequirementForm form = requirementFormRepository.findByLead(lead)
                .orElseThrow(() -> new BadRequestException("Requirement form not started yet"));
        if (form.getStatus() != RequirementFormStatus.SUBMITTED) {
            throw new ConflictException("Only a submitted requirement form can be approved");
        }
        form.setStatus(RequirementFormStatus.APPROVED);
        form.setApprovedAt(Instant.now());
        form.setUpdatedAt(Instant.now());
        form = requirementFormRepository.save(form);
        leadActivityRepository.save(new LeadActivity(lead, customer, ActivityType.SYSTEM,
                "Customer approved the requirement form"));
        rescore(lead, form);
        return RequirementFormDto.from(form);
    }

    /** A staff edit to a customer-approved scope drops it back to SUBMITTED for re-approval. */
    private void unfreezeIfApproved(Lead lead, RequirementForm form) {
        if (form.getStatus() == RequirementFormStatus.APPROVED) {
            form.setStatus(RequirementFormStatus.SUBMITTED);
            form.setApprovedAt(null);
            leadActivityRepository.save(new LeadActivity(lead, null, ActivityType.SYSTEM,
                    "Project scope changed after customer approval — re-approval needed"));
        }
    }

    /** The form locks once the customer approved it or a proposal is on (or past) their table. */
    private void assertNotLocked(Lead lead) {
        if (requirementFormRepository.findByLead(lead)
                .map(form -> form.getStatus() == RequirementFormStatus.APPROVED).orElse(false)) {
            throw new ConflictException("Requirements can no longer be edited once you have approved them");
        }
        if (quoteRepository.existsByLeadAndStatusIn(lead,
                List.of(QuoteStatus.SENT, QuoteStatus.APPROVED))) {
            throw new ConflictException("Requirements can no longer be edited once a proposal has been sent");
        }
    }

    private void rescore(Lead lead, RequirementForm form) {
        scoreService.rescore(lead, form.getStatus());
        leadRepository.save(lead);
    }

    private void applyScalars(RequirementForm form, RequirementFormRequest r) {
        form.setProjectSegment(r.projectSegment());
        form.setSpaceType(r.spaceType());
        form.setProjectType(r.projectType());
        form.setScopeOfWork(r.scopeOfWork());
        form.setNumberOfFloors(r.numberOfFloors());
        form.setTotalAreaSqft(r.totalAreaSqft());
        form.setDesiredStartDate(r.desiredStartDate());
        form.setDesiredCompletionDate(r.desiredCompletionDate());
        form.setOccupancyStatus(r.occupancyStatus());
        form.setRenovationReason(r.renovationReason());
        form.setTotalAdults(r.totalAdults());
        form.setSeniorCitizens(r.seniorCitizens());
        form.setKidsDetails(r.kidsDetails());
        form.setWfhMembers(r.wfhMembers());
        form.setGuestFrequency(r.guestFrequency());
        form.setAccessibilityNeeds(r.accessibilityNeeds());
        form.setAllergies(r.allergies());
        form.setPetsKids(r.petsKids());
        form.setPreferredStyle(r.preferredStyle());
        form.setSecondaryStyle(r.secondaryStyle());
        form.setStylesToAvoid(r.stylesToAvoid());
        form.setInspirationLinks(r.inspirationLinks());
        form.setColourPalette(r.colourPalette());
        form.setWoodTone(r.woodTone());
        form.setMetalFinish(r.metalFinish());
        form.setFlooringPreference(r.flooringPreference());
        form.setSustainabilityPreference(r.sustainabilityPreference());
        form.setMaterialsToAvoid(r.materialsToAvoid());
        form.setLocalHandcraftedPreference(r.localHandcraftedPreference());
        form.setBudgetRange(r.budgetRange());
        form.setBudgetFlexibility(r.budgetFlexibility());
        form.setPaymentMilestonePreference(r.paymentMilestonePreference());
        form.setClientSourcedItems(r.clientSourcedItems());
        form.setPriorityAreas(r.priorityAreas());
        form.setTargetMoveInDate(r.targetMoveInDate());
        form.setFixedDeadlines(r.fixedDeadlines());
        form.setSiteAccess(r.siteAccess());
        form.setSocietyApproval(r.societyApproval());
        form.setStructuralChanges(r.structuralChanges());
        form.setElevatorRestrictions(r.elevatorRestrictions());
    }
}
