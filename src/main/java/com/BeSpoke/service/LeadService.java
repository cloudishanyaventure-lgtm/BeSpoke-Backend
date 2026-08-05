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
import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.CompanyType;
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
import com.BeSpoke.repository.CompanyRepository;
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
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class LeadService {

    /** Stages the assigned-only roles may move a lead between. WON/LOST need seniority. */
    private static final Set<LeadStatus> FUNNEL_STAGES = EnumSet.of(
            LeadStatus.CONTACTED, LeadStatus.SITE_VISIT, LeadStatus.PROPOSAL_SENT, LeadStatus.NEGOTIATION);

    /** Roles scoped to their own leads (assignment / sales ownership), not the company book. */
    private static final Set<Role> ASSIGNED_ONLY_ROLES = EnumSet.of(
            Role.DESIGNER, Role.PROJECT_MANAGER, Role.CUSTOMER_CONSULTANT, Role.SALES_EXECUTIVE);

    /**
     * Who may be put on a lead. Seniors are included deliberately: a small studio may have
     * no designer or no consultant, and the work still has to belong to someone. Same
     * principle as the approval chain, which skips roles the company does not staff.
     */
    private static final Set<Role> DESIGN_ASSIGNEES = EnumSet.of(
            Role.DESIGNER, Role.PROJECT_MANAGER, Role.DESIGN_MANAGER,
            Role.PRINCIPAL_ARCHITECT, Role.DIRECTOR);

    private static final Set<Role> SALES_ASSIGNEES = EnumSet.of(
            Role.CUSTOMER_CONSULTANT, Role.SALES_EXECUTIVE, Role.SALES_MANAGER, Role.DIRECTOR);

    private final LeadRepository leadRepository;
    private final LeadActivityRepository leadActivityRepository;
    private final RequirementFormRepository requirementFormRepository;
    private final QuoteRepository quoteRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMilestoneRepository projectMilestoneRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final ScoreService scoreService;
    private final ProjectService projectService;
    private final AuditService auditService;
    private final MailService mailService;

    public LeadService(LeadRepository leadRepository,
                       LeadActivityRepository leadActivityRepository,
                       RequirementFormRepository requirementFormRepository,
                       QuoteRepository quoteRepository,
                       ProjectRepository projectRepository,
                       ProjectMilestoneRepository projectMilestoneRepository,
                       UserRepository userRepository,
                       CompanyRepository companyRepository,
                       ScoreService scoreService,
                       ProjectService projectService,
                       AuditService auditService,
                       MailService mailService) {
        this.leadRepository = leadRepository;
        this.leadActivityRepository = leadActivityRepository;
        this.requirementFormRepository = requirementFormRepository;
        this.quoteRepository = quoteRepository;
        this.projectRepository = projectRepository;
        this.projectMilestoneRepository = projectMilestoneRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.scoreService = scoreService;
        this.projectService = projectService;
        this.auditService = auditService;
        this.mailService = mailService;
    }

    /**
     * Visibility matrix, used by every staff-facing service: platform sees
     * everything (incl. the pool), company-wide design roles see their studio's
     * book, DESIGNER/PROJECT_MANAGER their assigned leads, consultants/sales
     * execs the leads they own or are assigned to. Vendor companies have no leads.
     */
    public boolean canSee(User current, Lead lead) {
        Role role = current.getRole();
        if (role.isPlatform()) {
            return true;
        }
        if (current.getCompany() != null && current.getCompany().getType() != CompanyType.DESIGN) {
            return false;
        }
        if (role == Role.DESIGNER || role == Role.PROJECT_MANAGER) {
            return isAssignedDesigner(current, lead);
        }
        if (role == Role.CUSTOMER_CONSULTANT || role == Role.SALES_EXECUTIVE) {
            return isSalesOwner(current, lead) || isAssignedDesigner(current, lead);
        }
        if (role.seesWholeCompany()) {
            return lead.getCompany() != null && current.getCompany() != null
                    && lead.getCompany().getId().equals(current.getCompany().getId());
        }
        return false;
    }

    private static boolean isAssignedDesigner(User current, Lead lead) {
        return lead.getAssignedDesigner() != null
                && lead.getAssignedDesigner().getId().equals(current.getId());
    }

    private static boolean isSalesOwner(User current, Lead lead) {
        return lead.getSalesOwner() != null
                && lead.getSalesOwner().getId().equals(current.getId());
    }

    /** All leads the current staff user may see, newest first. Mirrors canSee. */
    public List<Lead> visibleLeads(User current) {
        Role role = current.getRole();
        if (role.isPlatform()) {
            return leadRepository.findAllByOrderByCreatedAtDesc();
        }
        if (current.getCompany() != null && current.getCompany().getType() != CompanyType.DESIGN) {
            return List.of();
        }
        if (role == Role.DESIGNER || role == Role.PROJECT_MANAGER) {
            return leadRepository.findByAssignedDesignerOrderByCreatedAtDesc(current);
        }
        if (role == Role.CUSTOMER_CONSULTANT || role == Role.SALES_EXECUTIVE) {
            return leadRepository.findBySalesOwnerOrAssignedDesignerOrderByCreatedAtDesc(current, current);
        }
        if (role.seesWholeCompany()) {
            return current.getCompany() == null ? List.of()
                    : leadRepository.findByCompanyOrderByCreatedAtDesc(current.getCompany());
        }
        return List.of();
    }

    /** Loads a lead the current staff user may see; 404 hides other studios' leads. */
    public Lead scopedLead(User current, Long leadId) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new NotFoundException("Lead not found"));
        if (!canSee(current, lead)) {
            throw new NotFoundException("Lead not found");
        }
        return lead;
    }

    public List<LeadSummaryDto> list(User current, String stage, String q, Long assignedId) {
        List<Lead> leads = visibleLeads(current);
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
        // Commercial terms follow the /api/quotes ACL — designers, PMs, architects and
        // sales execs see the brief and the funnel, never the rates.
        List<QuoteDto> quotes = current.getRole().seesQuotes()
                ? quoteRepository.findByLeadOrderByVersionDesc(lead).stream().map(QuoteDto::from).toList()
                : List.of();
        boolean finance = current.getRole().seesFinance();
        com.BeSpoke.dto.ProjectDto project = projectRepository.findByLead(lead)
                .map(p -> projectService.toDto(p, finance, false))
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
        if (lead.isApprovalPending()) {
            throw new BadRequestException("This lead is awaiting approval");
        }
        LeadStatus target = parseStatus(request.stage());
        LeadStatus from = lead.getStatus();
        if (target == from) {
            throw new BadRequestException("Lead is already in stage " + target.name());
        }
        if (from == LeadStatus.WON || from == LeadStatus.LOST) {
            throw new BadRequestException("A " + from.name() + " lead cannot change stage");
        }
        Role role = current.getRole();
        if (ASSIGNED_ONLY_ROLES.contains(role) && !FUNNEL_STAGES.contains(target)) {
            throw new ForbiddenException("You may only move leads between CONTACTED and NEGOTIATION");
        }
        if ((target == LeadStatus.WON || target == LeadStatus.LOST)
                && !(role.isPlatform() || role == Role.DIRECTOR || role == Role.SALES_MANAGER)) {
            throw new ForbiddenException("Only a director or sales manager may close a lead");
        }
        if (target == LeadStatus.WON) {
            // Bypass: solo/small studios with the DESIGNER role disabled can win without one.
            boolean designerEnabled = lead.getCompany() == null
                    || lead.getCompany().effectiveEnabledRoles().contains(Role.DESIGNER);
            if (designerEnabled && lead.getAssignedDesigner() == null) {
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
    public LeadSummaryDto assign(User actor, Long leadId, Long designerId) {
        Lead lead = scopedLead(actor, leadId);
        User designer = requireAssignee(lead, designerId, DESIGN_ASSIGNEES, "designer");
        lead.setAssignedDesigner(designer);
        lead.setUpdatedAt(Instant.now());
        leadRepository.save(lead);
        leadActivityRepository.save(new LeadActivity(lead, actor, ActivityType.SYSTEM,
                "Assigned to " + designer.getName()));
        return toSummary(lead);
    }

    /** Director/sales manager hands the customer relationship to a consultant or sales exec. */
    @Transactional
    public LeadSummaryDto assignSales(User actor, Long leadId, Long userId) {
        Lead lead = scopedLead(actor, leadId);
        User owner = requireAssignee(lead, userId, SALES_ASSIGNEES, "sales owner");
        lead.setSalesOwner(owner);
        lead.setUpdatedAt(Instant.now());
        leadRepository.save(lead);
        leadActivityRepository.save(new LeadActivity(lead, actor, ActivityType.SYSTEM,
                "Sales owner set to " + owner.getName()));
        return toSummary(lead);
    }

    /** Loads an active same-studio staffer of one of the given roles (unrouted leads adopt their studio). */
    private User requireAssignee(Lead lead, Long userId, Set<Role> roles, String label) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!roles.contains(user.getRole())) {
            throw new BadRequestException("Selected user cannot be the " + label);
        }
        if (!user.isActive()) {
            throw new BadRequestException("Selected user is deactivated");
        }
        if (lead.getCompany() == null) {
            lead.setCompany(user.getCompany());
        } else if (user.getCompany() == null
                || !user.getCompany().getId().equals(lead.getCompany().getId())) {
            throw new BadRequestException("Selected user belongs to a different studio");
        }
        return user;
    }

    /**
     * Platform transfers a lead to a design company: the receiving studio must
     * accept it before working it. Re-transfer clears assignments and acceptance.
     */
    @Transactional
    public LeadSummaryDto route(User admin, Long leadId, Long companyId) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new NotFoundException("Lead not found"));
        if (!admin.getRole().isPlatform()) {
            throw new ForbiddenException("Only BeSpoke can transfer leads");
        }
        Company company = companyRepository.findById(companyId)
                .filter(Company::canTakeLeads)
                .orElseThrow(() -> new NotFoundException("Studio not found, inactive or not KYC-verified"));
        lead.setCompany(company);
        lead.setTransferredAt(Instant.now());
        lead.setAcceptedAt(null);
        lead.setAcceptedBy(null);
        lead.setAssignedDesigner(null);
        lead.setSalesOwner(null);
        lead.setUpdatedAt(Instant.now());
        leadRepository.save(lead);
        leadActivityRepository.save(new LeadActivity(lead, admin, ActivityType.SYSTEM,
                "Transferred to " + company.getName() + " by BeSpoke"));
        auditService.log(admin, company, "LEAD_TRANSFERRED",
                "Lead #" + lead.getId() + " (" + lead.getContactName() + ") transferred to " + company.getName());
        // Tell the studio's decision makers there is something to accept.
        Stream.of(Role.DIRECTOR, Role.SALES_MANAGER)
                .flatMap(role -> userRepository.findByCompanyAndRole(company, role).stream())
                .filter(User::isActive)
                .forEach(user -> mailService.leadRouted(user, lead));
        return toSummary(lead);
    }

    /** Director/sales manager of the receiving studio accepts a transferred lead. */
    @Transactional
    public LeadSummaryDto accept(User actor, Long leadId) {
        Lead lead = scopedLead(actor, leadId);
        if (lead.getCompany() == null) {
            throw new BadRequestException("Lead has not been transferred to a studio yet");
        }
        if (lead.getAcceptedAt() != null) {
            throw new BadRequestException("Lead is already accepted");
        }
        lead.setAcceptedAt(Instant.now());
        lead.setAcceptedBy(actor);
        lead.setUpdatedAt(Instant.now());
        leadRepository.save(lead);
        leadActivityRepository.save(new LeadActivity(lead, actor, ActivityType.SYSTEM,
                lead.getCompany().getName() + " accepted this project"));
        auditService.log(actor, lead.getCompany(), "LEAD_ACCEPTED",
                "Lead #" + lead.getId() + " (" + lead.getContactName() + ") accepted by " + actor.getName());
        if (lead.getCustomer() != null) {
            mailService.leadAccepted(lead.getCustomer(), lead.getCompany().getName());
        }
        return toSummary(lead);
    }

    /** Manual lead capture (walk-in / phone / referral) - no user account. Staff leads land in their own studio. */
    @Transactional
    public LeadSummaryDto createManual(User actor, CreateLeadRequest request) {
        if (actor.getCompany() != null && actor.getCompany().getType() != CompanyType.DESIGN) {
            throw new ForbiddenException("Vendor companies do not work the design lead funnel");
        }
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
        lead.setCreatedByName(actor.getName());
        lead.setCreatedByRole(actor.getRole().name());
        // Design-side creators need a senior's sign-off on the capture.
        lead.setApprovalPending(actor.getRole() == Role.DESIGNER
                || actor.getRole() == Role.DESIGN_MANAGER);
        lead.setCompany(actor.getCompany()); // null for platform admins: route later
        if (actor.getCompany() != null) {
            // The studio captured this lead itself — nothing to hand over or accept.
            lead.setTransferredAt(Instant.now());
            lead.setAcceptedAt(Instant.now());
            lead.setAcceptedBy(actor);
        }
        scoreService.rescore(lead, null);
        lead = leadRepository.save(lead);
        leadActivityRepository.save(new LeadActivity(lead, actor, ActivityType.SYSTEM,
                "Lead captured manually (" + source.name() + ")"));
        return toSummary(lead);
    }

    /**
     * Senior sign-off on a designer/design-manager-captured lead. The controller gate
     * admits SUPER_ADMIN/ADMIN/DIRECTOR/PRINCIPAL_ARCHITECT/DESIGN_MANAGER; here a
     * design manager may only approve a designer's capture — never a peer's.
     */
    @Transactional
    public LeadSummaryDto approveCreation(User actor, Long leadId) {
        Lead lead = scopedLead(actor, leadId);
        if (actor.getRole() == Role.DESIGN_MANAGER
                && !Role.DESIGNER.name().equals(lead.getCreatedByRole())) {
            throw new ForbiddenException("A design manager may only approve designer-created leads");
        }
        lead.setApprovalPending(false);
        lead.setUpdatedAt(Instant.now());
        leadRepository.save(lead);
        leadActivityRepository.save(new LeadActivity(lead, actor, ActivityType.SYSTEM,
                "Lead creation approved by " + actor.getName()));
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
        if (request.companyId() != null) {
            // A studio pick is a preference only (V3 §0): every enquiry starts in the
            // BeSpoke pool and the platform routes it. Unknown/ineligible ids are ignored.
            companyRepository.findById(request.companyId())
                    .filter(Company::canTakeLeads)
                    .ifPresent(lead::setPreferredCompany);
        }
        scoreService.rescore(lead, null);
        lead = leadRepository.save(lead);
        leadActivityRepository.save(new LeadActivity(lead, null, ActivityType.SYSTEM,
                lead.getPreferredCompany() != null
                        ? "Enquiry received — prefers " + lead.getPreferredCompany().getName()
                        : "Enquiry received"));
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
