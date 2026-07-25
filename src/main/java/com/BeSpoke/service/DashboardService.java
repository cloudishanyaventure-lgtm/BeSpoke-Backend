package com.BeSpoke.service;

import org.springframework.transaction.annotation.Transactional;
import com.BeSpoke.dto.ActivityDto;
import com.BeSpoke.dto.AdminDashboardDto;
import com.BeSpoke.dto.DesignerDashboardDto;
import com.BeSpoke.dto.LeadSummaryDto;
import com.BeSpoke.dto.ProjectDto;
import com.BeSpoke.entity.Invoice;
import com.BeSpoke.entity.InvoicePayment;
import com.BeSpoke.entity.InvoiceStatus;
import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.LeadStatus;
import com.BeSpoke.entity.Project;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.StaffProfile;
import com.BeSpoke.entity.User;
import com.BeSpoke.repository.InvoicePaymentRepository;
import com.BeSpoke.repository.InvoiceRepository;
import com.BeSpoke.repository.LeadActivityRepository;
import com.BeSpoke.repository.LeadRepository;
import com.BeSpoke.repository.MessageRepository;
import com.BeSpoke.repository.ProjectRepository;
import com.BeSpoke.repository.StaffProfileRepository;
import com.BeSpoke.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final List<LeadStatus> CLOSED_STAGES = List.of(LeadStatus.WON, LeadStatus.LOST);

    private static final Map<String, Long> BUDGET_MIDPOINTS = Map.of(
            "UNDER_5L", 350_000L,
            "L5_10", 750_000L,
            "L10_25", 1_750_000L,
            "L25_50", 3_750_000L,
            "ABOVE_50L", 6_000_000L);

    private final LeadRepository leadRepository;
    private final LeadActivityRepository leadActivityRepository;
    private final ProjectRepository projectRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final LeadService leadService;
    private final ProjectService projectService;

    public DashboardService(LeadRepository leadRepository,
                            LeadActivityRepository leadActivityRepository,
                            ProjectRepository projectRepository,
                            InvoiceRepository invoiceRepository,
                            InvoicePaymentRepository invoicePaymentRepository,
                            MessageRepository messageRepository,
                            UserRepository userRepository,
                            StaffProfileRepository staffProfileRepository,
                            LeadService leadService,
                            ProjectService projectService) {
        this.leadRepository = leadRepository;
        this.leadActivityRepository = leadActivityRepository;
        this.projectRepository = projectRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoicePaymentRepository = invoicePaymentRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.leadService = leadService;
        this.projectService = projectService;
    }

    public AdminDashboardDto adminDashboard() {
        BigDecimal revenueCollected = invoicePaymentRepository.findAll().stream()
                .map(InvoicePayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal outstanding = BigDecimal.ZERO;
        for (Invoice invoice : invoiceRepository.findByStatusOrderByCreatedAtDesc(InvoiceStatus.SENT)) {
            BigDecimal paid = invoicePaymentRepository.findByInvoiceOrderByPaidAtAsc(invoice).stream()
                    .map(InvoicePayment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            outstanding = outstanding.add(
                    com.BeSpoke.dto.InvoiceDto.totalOf(invoice).subtract(paid));
        }

        BigDecimal pipelineValue = BigDecimal.ZERO;
        List<Lead> openLeads = leadRepository.findByStatusNotIn(CLOSED_STAGES);
        for (Lead lead : openLeads) {
            Long midpoint = lead.getBudgetBand() != null
                    ? BUDGET_MIDPOINTS.get(lead.getBudgetBand()) : null;
            if (midpoint != null) {
                pipelineValue = pipelineValue.add(BigDecimal.valueOf(midpoint));
            }
        }

        Map<String, Long> leadsByStage = stageCounts(leadRepository.findAllByOrderByCreatedAtDesc());

        List<LeadSummaryDto> followUpsDue = leadRepository
                .findByFollowUpAtLessThanEqualAndStatusNotInOrderByFollowUpAtAsc(LocalDate.now(), CLOSED_STAGES)
                .stream().map(leadService::toSummary).toList();

        Map<String, Long> projectHealth = new LinkedHashMap<>();
        for (Project project : projectRepository.findAllByOrderByCreatedAtDesc()) {
            projectHealth.merge(project.getHealth().name(), 1L, Long::sum);
        }

        List<ActivityDto> recentActivities = leadActivityRepository.findTop15ByOrderByCreatedAtDesc()
                .stream().map(ActivityDto::from).toList();

        List<AdminDashboardDto.TeamLoadDto> teamLoad = new ArrayList<>();
        for (User designer : userRepository.findByRole(Role.DESIGNER)) {
            if (!designer.isActive()) {
                continue;
            }
            String title = staffProfileRepository.findByUser(designer)
                    .map(StaffProfile::getTitle).orElse(null);
            teamLoad.add(new AdminDashboardDto.TeamLoadDto(
                    designer.getId(),
                    designer.getName(),
                    title,
                    leadRepository.countByAssignedDesignerAndStatusNotIn(designer, CLOSED_STAGES),
                    projectRepository.countByDesigner(designer)));
        }

        return new AdminDashboardDto(revenueCollected, outstanding, pipelineValue, leadsByStage,
                followUpsDue, projectHealth, recentActivities, teamLoad);
    }

    public DesignerDashboardDto designerDashboard(User designer) {
        Map<String, Long> myLeadsByStage =
                stageCounts(leadRepository.findByAssignedDesignerOrderByCreatedAtDesc(designer));

        List<LeadSummaryDto> followUpsDue = leadRepository
                .findByFollowUpAtLessThanEqualAndStatusNotInAndAssignedDesignerOrderByFollowUpAtAsc(
                        LocalDate.now(), CLOSED_STAGES, designer)
                .stream().map(leadService::toSummary).toList();

        List<ProjectDto> myProjects = projectService.designerProjects(designer);

        long unreadMessages = messageRepository
                .countByLead_AssignedDesignerAndSenderNotAndReadAtIsNull(designer, designer);

        List<ActivityDto> recentActivities = leadActivityRepository
                .findTop15ByLead_AssignedDesignerOrderByCreatedAtDesc(designer)
                .stream().map(ActivityDto::from).toList();

        return new DesignerDashboardDto(myLeadsByStage, followUpsDue, myProjects,
                unreadMessages, recentActivities);
    }

    private Map<String, Long> stageCounts(List<Lead> leads) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (LeadStatus status : LeadStatus.values()) {
            counts.put(status.name(), 0L);
        }
        for (Lead lead : leads) {
            counts.merge(lead.getStatus().name(), 1L, Long::sum);
        }
        return counts;
    }
}
