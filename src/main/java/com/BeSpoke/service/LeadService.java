package com.BeSpoke.service;

import com.BeSpoke.dto.AdminOverviewDto;
import com.BeSpoke.dto.DesignerStatsDto;
import com.BeSpoke.dto.EnquiryRequest;
import com.BeSpoke.dto.LeadDto;
import com.BeSpoke.entity.ChatThread;
import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.LeadStatus;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.ServiceCategory;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.exception.ForbiddenException;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.ChatThreadRepository;
import com.BeSpoke.repository.DesignServiceRepository;
import com.BeSpoke.repository.DesignerProfileRepository;
import com.BeSpoke.repository.LeadRepository;
import com.BeSpoke.repository.OrderRepository;
import com.BeSpoke.repository.PaymentRepository;
import com.BeSpoke.repository.ReviewRepository;
import com.BeSpoke.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LeadService {

    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final ChatThreadRepository chatThreadRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final DesignServiceRepository designServiceRepository;
    private final DesignerProfileRepository designerProfileRepository;
    private final ReviewRepository reviewRepository;

    public LeadService(LeadRepository leadRepository,
                       UserRepository userRepository,
                       ChatThreadRepository chatThreadRepository,
                       PaymentRepository paymentRepository,
                       OrderRepository orderRepository,
                       DesignServiceRepository designServiceRepository,
                       DesignerProfileRepository designerProfileRepository,
                       ReviewRepository reviewRepository) {
        this.leadRepository = leadRepository;
        this.userRepository = userRepository;
        this.chatThreadRepository = chatThreadRepository;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.designServiceRepository = designServiceRepository;
        this.designerProfileRepository = designerProfileRepository;
        this.reviewRepository = reviewRepository;
    }

    /** Rule 3: free enquiry -> admin queue (status ENQUIRY). Anonymous allowed. */
    @Transactional
    public LeadDto createEnquiry(EnquiryRequest request, User customerOrNull) {
        Lead lead = new Lead();
        lead.setCustomer(customerOrNull);
        lead.setStatus(LeadStatus.ENQUIRY);
        lead.setContactName(request.name());
        lead.setContactEmail(request.email());
        lead.setContactPhone(request.phone());
        lead.setMessage(request.message());
        if (request.category() != null && !request.category().isBlank()) {
            try {
                lead.setCategory(ServiceCategory.valueOf(request.category().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Unknown category: " + request.category());
            }
        }
        return LeadDto.from(leadRepository.save(lead));
    }

    // ---- Customer ----

    @Transactional(readOnly = true)
    public List<LeadDto> myProjects(User customer) {
        return leadRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())
                .stream().map(LeadDto::from).toList();
    }

    // ---- Designer ----

    @Transactional(readOnly = true)
    public List<LeadDto> designerPendingLeads(User designer) {
        return leadRepository.findByDesignerIdAndStatusOrderByCreatedAtDesc(designer.getId(), LeadStatus.ASSIGNED)
                .stream().map(LeadDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<LeadDto> designerProjects(User designer) {
        return leadRepository.findByDesignerIdAndStatusInOrderByCreatedAtDesc(
                        designer.getId(),
                        EnumSet.of(LeadStatus.APPROVED, LeadStatus.IN_PROGRESS, LeadStatus.COMPLETED))
                .stream().map(LeadDto::from).toList();
    }

    /** Designer approves an assigned lead: status APPROVED + chat thread opens. */
    @Transactional
    public LeadDto approveLead(User designer, Long leadId) {
        Lead lead = requireLead(leadId);
        requireAssignedTo(lead, designer);
        if (lead.getStatus() != LeadStatus.ASSIGNED) {
            throw new BadRequestException("Only leads in ASSIGNED state can be approved (current: "
                    + lead.getStatus() + ")");
        }
        lead.setStatus(LeadStatus.APPROVED);

        // Open the customer <-> designer chat thread
        if (lead.getCustomer() != null && chatThreadRepository.findByLeadId(lead.getId()).isEmpty()) {
            chatThreadRepository.save(new ChatThread(lead, lead.getCustomer(), designer));
        }
        return LeadDto.from(leadRepository.save(lead));
    }

    /** Designer rejects an assigned lead: it goes back to the admin queue. */
    @Transactional
    public LeadDto rejectLead(User designer, Long leadId) {
        Lead lead = requireLead(leadId);
        requireAssignedTo(lead, designer);
        if (lead.getStatus() != LeadStatus.ASSIGNED) {
            throw new BadRequestException("Only leads in ASSIGNED state can be rejected (current: "
                    + lead.getStatus() + ")");
        }
        lead.setDesigner(null);
        lead.setStatus(LeadStatus.REJECTED);
        return LeadDto.from(leadRepository.save(lead));
    }

    /** Designer moves an approved project forward: APPROVED -> IN_PROGRESS -> COMPLETED. */
    @Transactional
    public LeadDto updateDesignerLeadStatus(User designer, Long leadId, String status) {
        LeadStatus target;
        try {
            target = LeadStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unknown lead status: " + status);
        }
        if (target != LeadStatus.IN_PROGRESS && target != LeadStatus.COMPLETED) {
            throw new BadRequestException("Designers can only move leads to IN_PROGRESS or COMPLETED");
        }
        Lead lead = requireLead(leadId);
        requireAssignedTo(lead, designer);
        boolean allowed = (target == LeadStatus.IN_PROGRESS && lead.getStatus() == LeadStatus.APPROVED)
                || (target == LeadStatus.COMPLETED && lead.getStatus() == LeadStatus.IN_PROGRESS);
        if (!allowed) {
            throw new BadRequestException("Cannot move lead from " + lead.getStatus() + " to " + target);
        }
        lead.setStatus(target);
        return LeadDto.from(leadRepository.save(lead));
    }

    @Transactional(readOnly = true)
    public DesignerStatsDto designerStats(User designer) {
        long profileViews = designerProfileRepository.findByUserId(designer.getId())
                .map(profile -> profile.getViewCount())
                .orElse(0L);
        EnumSet<LeadStatus> earningStatuses =
                EnumSet.of(LeadStatus.APPROVED, LeadStatus.IN_PROGRESS, LeadStatus.COMPLETED);
        BigDecimal totalEarnings = leadRepository.sumOrderTotalsForDesigner(designer.getId(), earningStatuses);
        Double avgRating = reviewRepository.averageRatingForDesigner(designer.getId());
        if (avgRating == null) {
            avgRating = designerProfileRepository.findByUserId(designer.getId())
                    .map(profile -> profile.getRating())
                    .orElse(null);
        }
        return new DesignerStatsDto(
                profileViews,
                leadRepository.countByDesignerIdAndStatus(designer.getId(), LeadStatus.ASSIGNED),
                leadRepository.countByDesignerIdAndStatusIn(designer.getId(),
                        EnumSet.of(LeadStatus.APPROVED, LeadStatus.IN_PROGRESS)),
                leadRepository.countByDesignerIdAndStatus(designer.getId(), LeadStatus.COMPLETED),
                totalEarnings != null ? totalEarnings : BigDecimal.ZERO,
                avgRating,
                reviewRepository.countByDesignerId(designer.getId())
        );
    }

    // ---- Admin ----

    @Transactional(readOnly = true)
    public List<LeadDto> adminLeads(String status) {
        if (status == null || status.isBlank()) {
            return leadRepository.findAllByOrderByCreatedAtDesc().stream().map(LeadDto::from).toList();
        }
        LeadStatus leadStatus;
        try {
            leadStatus = LeadStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unknown lead status: " + status);
        }
        return leadRepository.findByStatusOrderByCreatedAtDesc(leadStatus).stream().map(LeadDto::from).toList();
    }

    /** Admin assigns a designer to a lead (ENQUIRY / UNASSIGNED_PAID / REJECTED). */
    @Transactional
    public LeadDto assignDesigner(Long leadId, Long designerId) {
        Lead lead = requireLead(leadId);
        if (lead.getStatus() != LeadStatus.ENQUIRY
                && lead.getStatus() != LeadStatus.UNASSIGNED_PAID
                && lead.getStatus() != LeadStatus.REJECTED) {
            throw new BadRequestException("Lead cannot be assigned in state " + lead.getStatus());
        }
        User designer = userRepository.findById(designerId)
                .orElseThrow(() -> new NotFoundException("Designer not found: " + designerId));
        if (designer.getRole() != Role.DESIGNER) {
            throw new BadRequestException("User " + designerId + " is not a designer");
        }
        lead.setDesigner(designer);
        lead.setStatus(LeadStatus.ASSIGNED);

        // Route the pending payout to the newly assigned designer, if the lead is backed by a paid order.
        if (lead.getOrder() != null) {
            paymentRepository.findByOrderId(lead.getOrder().getId()).ifPresent(payment -> {
                payment.setPayeeDesigner(designer);
                paymentRepository.save(payment);
            });
        }
        return LeadDto.from(leadRepository.save(lead));
    }

    @Transactional(readOnly = true)
    public AdminOverviewDto overview() {
        Map<String, Long> leadsByStatus = new LinkedHashMap<>();
        for (LeadStatus status : LeadStatus.values()) {
            leadsByStatus.put(status.name(), leadRepository.countByStatus(status));
        }
        return new AdminOverviewDto(
                userRepository.countByRole(Role.CUSTOMER),
                userRepository.countByRole(Role.DESIGNER),
                designServiceRepository.count(),
                orderRepository.count(),
                leadsByStatus
        );
    }

    // ---- helpers ----

    private Lead requireLead(Long leadId) {
        return leadRepository.findById(leadId)
                .orElseThrow(() -> new NotFoundException("Lead not found: " + leadId));
    }

    private void requireAssignedTo(Lead lead, User designer) {
        if (lead.getDesigner() == null || !lead.getDesigner().getId().equals(designer.getId())) {
            throw new ForbiddenException("This lead is not assigned to you");
        }
    }
}
