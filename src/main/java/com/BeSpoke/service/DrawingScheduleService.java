package com.BeSpoke.service;

import com.BeSpoke.dto.DrawingScheduleDto;
import com.BeSpoke.dto.DrawingScheduleRequest;
import com.BeSpoke.entity.DrawingSchedule;
import com.BeSpoke.entity.DrawingScheduleItem;
import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.Quote;
import com.BeSpoke.entity.QuoteItem;
import com.BeSpoke.entity.QuoteStatus;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.exception.ConflictException;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.DrawingScheduleRepository;
import com.BeSpoke.repository.QuoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The drawing timeline: built from the approved BOQ's elements, adjusted by the studio, then
 * signed off by an approver (design manager / director) before work is tracked against it.
 * Scoping and 404s go through {@link LeadService#scopedLead}. Editing is only allowed while
 * the schedule is not yet APPROVED; item progress is only tracked once it is.
 */
@Service
public class DrawingScheduleService {

    private final DrawingScheduleRepository schedules;
    private final QuoteRepository quotes;
    private final LeadService leadService;

    public DrawingScheduleService(DrawingScheduleRepository schedules, QuoteRepository quotes,
                                  LeadService leadService) {
        this.schedules = schedules;
        this.quotes = quotes;
        this.leadService = leadService;
    }

    @Transactional(readOnly = true)
    public DrawingScheduleDto get(User staff, Long leadId) {
        Lead lead = leadService.scopedLead(staff, leadId);
        Optional<Quote> boq = approvedBoq(lead);
        return schedules.findByLead(lead)
                .map(s -> DrawingScheduleDto.from(s, boq.isPresent()))
                .orElseGet(() -> proposal(lead, boq));
    }

    @Transactional
    public DrawingScheduleDto save(User staff, Long leadId, DrawingScheduleRequest req) {
        Lead lead = leadService.scopedLead(staff, leadId);
        DrawingSchedule s = schedules.findByLead(lead)
                .orElseGet(() -> new DrawingSchedule(lead, req.startDate(), req.endDate()));
        if (s.getStatus() == DrawingSchedule.Status.APPROVED) {
            throw new ConflictException("The schedule is approved and locked — reopen it to edit.");
        }
        if (req.endDate().isBefore(req.startDate())) {
            throw new BadRequestException("The end date must be on or after the start date.");
        }
        if (req.items() == null || req.items().isEmpty()) {
            throw new BadRequestException("Add at least one element to the schedule.");
        }
        for (DrawingScheduleRequest.ItemRequest ir : req.items()) {
            if (ir.endDate().isBefore(ir.startDate())) {
                throw new BadRequestException("An element's end date can't be before its start date.");
            }
            if (ir.startDate().isBefore(req.startDate()) || ir.endDate().isAfter(req.endDate())) {
                throw new BadRequestException("Every element must sit inside the project window.");
            }
        }
        s.setStartDate(req.startDate());
        s.setEndDate(req.endDate());
        // Editing is only possible before approval, when every item is still NOT_STARTED, so
        // rebuilding the list wholesale never discards recorded progress.
        s.getItems().clear();
        int order = 0;
        for (DrawingScheduleRequest.ItemRequest ir : req.items()) {
            DrawingScheduleItem it = new DrawingScheduleItem();
            it.setSchedule(s);
            it.setSection(ir.section() == null || ir.section().isBlank() ? null : ir.section().trim());
            it.setLabel(ir.label() == null ? "" : ir.label().trim());
            it.setOrderIndex(order++);
            it.setStartDate(ir.startDate());
            it.setEndDate(ir.endDate());
            s.getItems().add(it);
        }
        return DrawingScheduleDto.from(schedules.save(s), approvedBoq(lead).isPresent());
    }

    @Transactional
    public DrawingScheduleDto submit(User staff, Long leadId) {
        Lead lead = leadService.scopedLead(staff, leadId);
        DrawingSchedule s = schedules.findByLead(lead)
                .orElseThrow(() -> new BadRequestException("Save the schedule before submitting it."));
        if (s.getStatus() == DrawingSchedule.Status.APPROVED) {
            throw new ConflictException("The schedule is already approved.");
        }
        if (s.getItems().isEmpty()) {
            throw new BadRequestException("The schedule has no elements to submit.");
        }
        s.setStatus(DrawingSchedule.Status.SUBMITTED);
        s.setSubmittedByName(staff.getName());
        s.setSubmittedAt(Instant.now());
        return DrawingScheduleDto.from(schedules.save(s), approvedBoq(lead).isPresent());
    }

    @Transactional
    public DrawingScheduleDto approve(User approver, Long leadId) {
        Lead lead = leadService.scopedLead(approver, leadId);
        DrawingSchedule s = schedules.findByLead(lead)
                .orElseThrow(() -> new NotFoundException("There is no schedule to approve."));
        if (s.getStatus() != DrawingSchedule.Status.SUBMITTED) {
            throw new ConflictException("Only a submitted schedule can be approved.");
        }
        s.setStatus(DrawingSchedule.Status.APPROVED);
        s.setApprovedByName(approver.getName());
        s.setApprovedAt(Instant.now());
        return DrawingScheduleDto.from(schedules.save(s), approvedBoq(lead).isPresent());
    }

    /** An approver reopens an approved schedule so the dates can be adjusted again. */
    @Transactional
    public DrawingScheduleDto reopen(User approver, Long leadId) {
        Lead lead = leadService.scopedLead(approver, leadId);
        DrawingSchedule s = schedules.findByLead(lead)
                .orElseThrow(() -> new NotFoundException("There is no schedule to reopen."));
        s.setStatus(DrawingSchedule.Status.DRAFT);
        s.setApprovedByName(null);
        s.setApprovedAt(null);
        return DrawingScheduleDto.from(schedules.save(s), approvedBoq(lead).isPresent());
    }

    @Transactional
    public DrawingScheduleDto setItemStatus(User staff, Long leadId, Long itemId, String status) {
        Lead lead = leadService.scopedLead(staff, leadId);
        DrawingSchedule s = schedules.findByLead(lead)
                .orElseThrow(() -> new NotFoundException("There is no schedule."));
        if (s.getStatus() != DrawingSchedule.Status.APPROVED) {
            throw new ConflictException("Approve the schedule before tracking work against it.");
        }
        DrawingScheduleItem it = s.getItems().stream()
                .filter(x -> x.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> new NotFoundException("That element is not on this schedule."));
        DrawingScheduleItem.Status target;
        try {
            target = DrawingScheduleItem.Status.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown status.");
        }
        it.setStatus(target);
        switch (target) {
            case IN_PROGRESS -> {
                if (it.getStartedAt() == null) it.setStartedAt(Instant.now());
                it.setCompletedAt(null);
            }
            case COMPLETED -> {
                if (it.getStartedAt() == null) it.setStartedAt(Instant.now());
                it.setCompletedAt(Instant.now());
            }
            case NOT_STARTED -> {
                it.setStartedAt(null);
                it.setCompletedAt(null);
            }
        }
        return DrawingScheduleDto.from(schedules.save(s), approvedBoq(lead).isPresent());
    }

    // ---- helpers ----

    private Optional<Quote> approvedBoq(Lead lead) {
        return quotes.findByLeadAndStatusInOrderByVersionDesc(lead, List.of(QuoteStatus.APPROVED))
                .stream().findFirst();
    }

    /**
     * A non-persisted schedule seeded from the BOQ. Every room appears: a room with elements
     * contributes one unit per element (so more elements ⇒ more time), and a room with none —
     * a priced line the studio never broke down — contributes itself as a single unit. Without
     * this fallback, element-less rooms would silently drop off the timeline.
     */
    private DrawingScheduleDto proposal(Lead lead, Optional<Quote> boq) {
        List<DrawingScheduleDto.Item> items = new ArrayList<>();
        if (boq.isPresent()) {
            List<QuoteItem> qis = boq.get().getItems();
            int order = 0;
            int i = 0;
            while (i < qis.size()) {
                QuoteItem qi = qis.get(i);
                if (qi.isHeading()) {
                    String section = qi.getSection();
                    // Elements that belong to this room: contiguous, non-heading, same section.
                    List<QuoteItem> children = new ArrayList<>();
                    int j = i + 1;
                    while (j < qis.size() && !qis.get(j).isHeading()
                            && section != null && section.equals(qis.get(j).getSection())) {
                        children.add(qis.get(j));
                        j++;
                    }
                    if (children.isEmpty()) {
                        items.add(item(section, qi.getDescription(), order++));
                    } else {
                        for (QuoteItem c : children) {
                            items.add(item(c.getSection(), c.getDescription(), order++));
                        }
                    }
                    i = j;
                } else {
                    // A hand-typed line with no room above it — schedule it on its own.
                    items.add(item(qi.getSection(), qi.getDescription(), order++));
                    i++;
                }
            }
        }
        return new DrawingScheduleDto(null, lead.getId(), null, null, "DRAFT", true,
                null, null, null, null, items, boq.isPresent());
    }

    private static DrawingScheduleDto.Item item(String section, String label, int order) {
        return new DrawingScheduleDto.Item(null, section, label, order, null, null,
                "NOT_STARTED", null, null);
    }
}
