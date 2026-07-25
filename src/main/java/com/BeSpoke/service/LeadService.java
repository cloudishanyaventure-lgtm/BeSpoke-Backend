package com.BeSpoke.service;

import com.BeSpoke.dto.ActivityDto;
import com.BeSpoke.dto.CreateActivityRequest;
import com.BeSpoke.dto.CreateLeadRequest;
import com.BeSpoke.dto.EnquiryRequest;
import com.BeSpoke.dto.LeadDetailDto;
import com.BeSpoke.dto.LeadSummaryDto;
import com.BeSpoke.dto.QuoteDto;
import com.BeSpoke.dto.StageChangeRequest;
import com.BeSpoke.entity.ActivityType;
import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.LeadActivity;
import com.BeSpoke.entity.LeadSource;
import com.BeSpoke.entity.LeadStatus;
import com.BeSpoke.entity.Project;
import com.BeSpoke.entity.ProjectMilestone;
import com.BeSpoke.entity.ProjectStage;
import com.BeSpoke.entity.RequirementForm;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.exception.ForbiddenException;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.LeadActivityRepository;
import com.BeSpoke.repository.LeadRepository;
import com.BeSpoke.repository.ProjectMilestoneRepository;
import com.BeSpoke.repository.ProjectRepository;
import com.BeSpoke.repository.QuoteRepository;
import com.BeSpoke.repository.RequirementFormRepository;
import com.BeSpoke.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class LeadService {

    /** Stages a DESIGNER may move an assigned lead between. WON/LOST are admin-only. */
    private static final Set<LeadStatus> DESIGNER_STAGES = EnumSet.of(
            LeadStatus.CONTACTED, LeadStatus.SITE_VISIT, LeadStatus.PROPOSAL_SENT, LeadStatus.NEGOTIATION);

    private final LeadRepository leadRepository;
    private final LeadActivityRepository leadActivityRepository;
    private final RequirementFormRepository requirementFormRepository;
    private final QuoteRepository quoteRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMilestoneRepository projectMilestoneRepository;
    private final UserRepository userRepository;
    private final ScoreService scoreService;
    private final ProjectService projectService;

    public LeadService(LeadRepository leadRepository,
                       LeadActivityRepository leadActivityRepository,
                       RequirementFormRepository requirementFormRepository,
                       QuoteRepository quoteRepository,
                       ProjectRepository projectRepository,
                       ProjectMilestoneRepository projectMilestoneRepository,
                       UserRepository userRepository,
                       ScoreService scoreService,
                       ProjectService projectService) {
        this.leadRepository = leadRepository;
        this.leadActivityRepository = leadActivityRepository;
        this.requirementFormRepository = requirementFormRepository;
        this.quoteRepository = quoteRepository;
        this.projectRepository = projectRepository;
        this.projectMilestoneRepository = projectMilestoneRepository;
        this.userRepository = userRepository;
        this.scoreService = scoreService;
        this.projectService = projectService;
    }

    /** Loads a lead the current staff user may see. Designers get 404 for leads not assigned to them. */
    public Lead scopedLead(User current, Long leadId) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new NotFoundException("Lead not found"));
        if (current.getRole() == Role.DESIGNER
                && (lead.getAssignedDesigner() == null
                    || !lead.getAssignedDesigner().getId().equals(current.getId()))) {
            throw new NotFoundException("Lead not found");
        }
        return lead;
    }

    public List<LeadSummaryDto> list(User current, String stage, String q, Long assignedId) {
        List<Lead> leads = current.getRole() == Role.DESIGNER
                ? leadRepository.findByAssignedDesignerOrderByCreatedAtDesc(current)
                : leadRepository.findAllByOrderByCreatedAtDesc();
        LeadStatus stageFilter = stage == null || stage.isBlank() ? null : parseStatus(stage);
        String query = q == null || q.isBlank() ? null : q.trim().toLowerCase(Locale.ROOT);
        return leads.stream()
                .filter(l -> stageFilter == null || l.getStatus() == stageFilter)
                .filter(l -> assignedId == null
                        || (l.getAssignedDesigner() != null && l.getAssignedDesigner().getId().equals(assignedId)))
                .filter(l -> query == null
                        || l.getContactName().toLowerCase(Locale.ROOT).contains(query)
                        || l.getContactEmail().toLowerCase(Locale.ROOT).contains(query)
                        || l.getContactPhone().toLowerCase(Locale.ROOT).contains(query)
                        || l.getCity().toLowerCase(Locale.ROOT).contains(query))
                .map(this::toSummary)
                .toList();
    }

    public LeadDetailDto detail(User current, Long leadId) {
        Lead lead = scopedLead(current, leadId);
        RequirementForm form = requirementFormRepository.findByLead(lead).orElse(null);
        List<ActivityDto> activities = leadActivityRepository.findByLeadOrderByCreatedAtAsc(lead)
                .stream().map(ActivityDto::from).toList();
        List<QuoteDto> quotes = quoteRepository.findByLeadOrderByVersionDesc(lead)
                .stream().map(QuoteDto::from).toList();
        boolean admin = current.getRole() == Role.ADMIN;
        com.BeSpoke.dto.ProjectDto project = projectRepository.findByLead(lead)
                .map(p -> projectService.toDto(p, admin, false))
                .orElse(null);
        return new LeadDetailDto(
                toSummary(lead),
                form != null ? com.BeSpoke.dto.RequirementFormDto.from(form) : null,
                activities,
                quotes,
                project);
    }

    @Transactional
    public ActivityDto addActivity(User current, Long leadId, CreateActivityRequest request) {
        Lead lead = scopedLead(current, leadId);
        LeadActivity activity = leadActivityRepository.save(new LeadActivity(
                lead, current, ActivityType.valueOf(request.type()), request.body().trim()));
        return ActivityDto.from(activity);
    }

    @Transactional
    public LeadSummaryDto changeStage(User current, Long leadId, StageChangeRequest request) {
        Lead lead = scopedLead(current, leadId);
        LeadStatus target = parseStatus(request.stage());
        LeadStatus from = lead.getStatus();
        if (target == from) {
            throw new BadRequestException("Lead is already in stage " + target.name());
        }
        if (from == LeadStatus.WON || from == LeadStatus.LOST) {
            throw new BadRequestException("A " + from.name() + " lead cannot change stage");
        }
        if (current.getRole() == Role.DESIGNER && !DESIGNER_STAGES.contains(target)) {
            throw new ForbiddenException("Designers may only move leads between CONTACTED and NEGOTIATION");
        }
        if (target == LeadStatus.WON) {
            if (lead.getAssignedDesigner() == null) {
                throw new BadRequestException("Assign a designer before marking this lead as won");
            }
            lead.setWonAt(Instant.now());
        }
        lead.setStatus(target);
        leadRepository.save(lead);
        leadActivityRepository.save(new LeadActivity(lead, current, ActivityType.STAGE,
                "Stage: " + from.name() + " → " + target.name()));
        if (target == LeadStatus.LOST && request.reason() != null && !request.reason().isBlank()) {
            leadActivityRepository.save(new LeadActivity(lead, current, ActivityType.NOTE,
                    "Lost reason: " + request.reason().trim()));
        }
        if (target == LeadStatus.WON && projectRepository.findByLead(lead).isEmpty()) {
            createProjectForWonLead(lead);
        }
        return toSummary(lead);
    }

    /** APARTMENT → "Apartment", BUILDER_FLOOR → "Builder floor"; null → "Home". */
    private static String humanizePropertyType(String propertyType) {
        if (propertyType == null || propertyType.isBlank()) {
            return "Home";
        }
        String lower = propertyType.replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private void createProjectForWonLead(Lead lead) {
        Project project = new Project();
        project.setLead(lead);
        project.setClient(lead.getCustomer());
        project.setDesigner(lead.getAssignedDesigner());
        project.setName(lead.getContactName() + " — " + humanizePropertyType(lead.getPropertyType()));
        project.setStage(ProjectStage.DESIGN_BRIEF);
        project.setHealth(com.BeSpoke.entity.ProjectHealth.ON_TRACK);
        project = projectRepository.save(project);
        int order = 0;
        for (ProjectStage stage : ProjectStage.values()) {
            projectMilestoneRepository.save(new ProjectMilestone(project, stage.getDisplayName(), null, order++));
        }
        leadActivityRepository.save(new LeadActivity(lead, null, ActivityType.SYSTEM,
                "Project \"" + project.getName() + "\" created"));
    }

    @Transactional
    public LeadSummaryDto setFollowUp(User current, Long leadId, LocalDate at) {
        Lead lead = scopedLead(current, leadId);
        lead.setFollowUpAt(at);
        lead.setUpdatedAt(Instant.now());
        leadRepository.save(lead);
        return toSummary(lead);
    }

    @Transactional
    public LeadSummaryDto assign(User admin, Long leadId, Long designerId) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new NotFoundException("Lead not found"));
        User designer = userRepository.findById(designerId)
                .orElseThrow(() -> new NotFoundException("Designer not found"));
        if (designer.getRole() != Role.DESIGNER) {
            throw new BadRequestException("Selected user is not a designer");
        }
        if (!designer.isActive()) {
            throw new BadRequestException("Selected designer is deactivated");
        }
        lead.setAssignedDesigner(designer);
        lead.setUpdatedAt(Instant.now());
        leadRepository.save(lead);
        leadActivityRepository.save(new LeadActivity(lead, admin, ActivityType.SYSTEM,
                "Assigned to " + designer.getName()));
        return toSummary(lead);
    }

    /** Admin manual lead capture (walk-in / phone / referral) - no user account. */
    @Transactional
    public LeadSummaryDto createManual(User admin, CreateLeadRequest request) {
        LeadSource source;
        try {
            source = LeadSource.valueOf(request.source().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unknown lead source: " + request.source());
        }
        Lead lead = new Lead();
        lead.setContactName(request.name().trim());
        lead.setContactEmail(request.email().toLowerCase().trim());
        lead.setContactPhone(request.phone().trim());
        lead.setCity(request.city().trim());
        lead.setPropertyType(request.propertyType());
        lead.setBudgetBand(request.budgetBand());
        lead.setSource(source);
        lead.setStatus(LeadStatus.NEW_INQUIRY);
        scoreService.rescore(lead, null);
        lead = leadRepository.save(lead);
        leadActivityRepository.save(new LeadActivity(lead, admin, ActivityType.SYSTEM,
                "Lead captured manually (" + source.name() + ")"));
        return toSummary(lead);
    }

    /** Public enquiry form - creates a lead without a user account. */
    @Transactional
    public Long createEnquiry(EnquiryRequest request) {
        Lead lead = new Lead();
        lead.setContactName(request.name().trim());
        lead.setContactEmail(request.email().toLowerCase().trim());
        lead.setContactPhone(request.phone().trim());
        lead.setCity(request.city().trim());
        lead.setPropertyType(request.propertyType());
        lead.setBudgetBand(request.budgetBand());
        lead.setSource(LeadSource.ENQUIRY);
        lead.setStatus(LeadStatus.NEW_INQUIRY);
        scoreService.rescore(lead, null);
        lead = leadRepository.save(lead);
        leadActivityRepository.save(new LeadActivity(lead, null, ActivityType.SYSTEM, "Enquiry received"));
        if (request.message() != null && !request.message().isBlank()) {
            leadActivityRepository.save(new LeadActivity(lead, null, ActivityType.NOTE,
                    "Enquiry message: " + request.message().trim()));
        }
        return lead.getId();
    }

    public LeadSummaryDto toSummary(Lead lead) {
        String formStatus = requirementFormRepository.findByLead(lead)
                .map(f -> f.getStatus().name()).orElse(null);
        return LeadSummaryDto.from(lead, formStatus);
    }

    private LeadStatus parseStatus(String value) {
        try {
            return LeadStatus.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unknown lead stage: " + value);
        }
    }
}
