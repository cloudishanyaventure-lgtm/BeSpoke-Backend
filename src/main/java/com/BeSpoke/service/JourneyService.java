package com.BeSpoke.service;

import org.springframework.transaction.annotation.Transactional;
import com.BeSpoke.dto.JourneyDto;
import com.BeSpoke.dto.ProjectDto;
import com.BeSpoke.entity.ActivityType;
import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.LeadActivity;
import com.BeSpoke.entity.Project;
import com.BeSpoke.entity.QuoteStatus;
import com.BeSpoke.entity.RequirementForm;
import com.BeSpoke.entity.StaffProfile;
import com.BeSpoke.entity.User;
import com.BeSpoke.repository.InvoiceRepository;
import com.BeSpoke.repository.LeadActivityRepository;
import com.BeSpoke.repository.MessageRepository;
import com.BeSpoke.repository.ProjectRepository;
import com.BeSpoke.repository.QuoteRepository;
import com.BeSpoke.repository.RequirementFormRepository;
import com.BeSpoke.repository.StaffProfileRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Builds the customer's read-only journey view. */
@Service
@Transactional(readOnly = true)
public class JourneyService {

    private static final List<QuoteStatus> CUSTOMER_VISIBLE_QUOTES = List.of(
            QuoteStatus.SENT, QuoteStatus.APPROVED, QuoteStatus.CHANGES_REQUESTED);

    private final RequirementService requirementService;
    private final RequirementFormRepository requirementFormRepository;
    private final LeadActivityRepository leadActivityRepository;
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final QuoteRepository quoteRepository;
    private final InvoiceRepository invoiceRepository;
    private final MessageRepository messageRepository;
    private final StaffProfileRepository staffProfileRepository;

    public JourneyService(RequirementService requirementService,
                          RequirementFormRepository requirementFormRepository,
                          LeadActivityRepository leadActivityRepository,
                          ProjectRepository projectRepository,
                          ProjectService projectService,
                          QuoteRepository quoteRepository,
                          InvoiceRepository invoiceRepository,
                          MessageRepository messageRepository,
                          StaffProfileRepository staffProfileRepository) {
        this.requirementService = requirementService;
        this.requirementFormRepository = requirementFormRepository;
        this.leadActivityRepository = leadActivityRepository;
        this.projectRepository = projectRepository;
        this.projectService = projectService;
        this.quoteRepository = quoteRepository;
        this.invoiceRepository = invoiceRepository;
        this.messageRepository = messageRepository;
        this.staffProfileRepository = staffProfileRepository;
    }

    public JourneyDto journey(User customer) {
        Lead lead = requirementService.myLead(customer);

        // Stage history: creation event plus every STAGE activity, in order.
        List<JourneyDto.StageEventDto> stageHistory = new ArrayList<>();
        stageHistory.add(new JourneyDto.StageEventDto(
                com.BeSpoke.entity.LeadStatus.NEW_INQUIRY.name(), lead.getCreatedAt()));
        for (LeadActivity activity : leadActivityRepository
                .findByLeadAndTypeOrderByCreatedAtAsc(lead, ActivityType.STAGE)) {
            String body = activity.getBody();
            int arrow = body.lastIndexOf("→");
            if (arrow >= 0) {
                stageHistory.add(new JourneyDto.StageEventDto(
                        body.substring(arrow + 1).trim(), activity.getCreatedAt()));
            }
        }

        JourneyDto.DesignerCardDto designerCard = null;
        User designer = lead.getAssignedDesigner();
        if (designer != null) {
            String title = staffProfileRepository.findByUser(designer)
                    .map(StaffProfile::getTitle).orElse("Designer");
            designerCard = new JourneyDto.DesignerCardDto(designer.getName(), title, designer.getPhone());
        }

        Project project = projectRepository.findByLead(lead).orElse(null);
        ProjectDto projectDto = project != null ? projectService.toDto(project, false, true) : null;

        RequirementForm form = requirementFormRepository.findByLead(lead).orElse(null);

        long quoteCount = quoteRepository
                .findByLeadAndStatusInOrderByVersionDesc(lead, CUSTOMER_VISIBLE_QUOTES).size();
        long pendingQuoteCount = quoteRepository
                .findByLeadAndStatusInOrderByVersionDesc(lead, List.of(QuoteStatus.SENT)).size();
        long invoiceCount = project != null
                ? invoiceRepository.findByProjectOrderByCreatedAtAsc(project).stream()
                        .filter(i -> i.getStatus() != com.BeSpoke.entity.InvoiceStatus.DRAFT).count()
                : 0;
        long unreadMessages = messageRepository.countByLeadAndSenderNotAndReadAtIsNull(lead, customer);

        return new JourneyDto(
                new JourneyDto.LeadJourneyDto(
                        lead.getId(), lead.getStatus().name(), lead.getCreatedAt(), stageHistory),
                designerCard,
                projectDto,
                form != null,
                form != null ? form.getStatus().name() : null,
                quoteCount,
                pendingQuoteCount,
                invoiceCount,
                unreadMessages);
    }
}
