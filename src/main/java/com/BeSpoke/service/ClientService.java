package com.BeSpoke.service;

import org.springframework.transaction.annotation.Transactional;
import com.BeSpoke.dto.ClientDetailDto;
import com.BeSpoke.dto.ClientDto;
import com.BeSpoke.dto.InvoiceDto;
import com.BeSpoke.dto.LeadSummaryDto;
import com.BeSpoke.dto.ProjectDto;
import com.BeSpoke.dto.QuoteDto;
import com.BeSpoke.entity.Invoice;
import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.Project;
import com.BeSpoke.entity.RequirementFormStatus;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.InvoicePaymentRepository;
import com.BeSpoke.repository.InvoiceRepository;
import com.BeSpoke.repository.LeadRepository;
import com.BeSpoke.repository.ProjectRepository;
import com.BeSpoke.repository.QuoteRepository;
import com.BeSpoke.repository.RequirementFormRepository;
import com.BeSpoke.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ClientService {

    private final UserRepository userRepository;
    private final LeadRepository leadRepository;
    private final ProjectRepository projectRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;
    private final QuoteRepository quoteRepository;
    private final RequirementFormRepository requirementFormRepository;
    private final LeadService leadService;
    private final ProjectService projectService;
    private final InvoiceService invoiceService;

    public ClientService(UserRepository userRepository,
                         LeadRepository leadRepository,
                         ProjectRepository projectRepository,
                         InvoiceRepository invoiceRepository,
                         InvoicePaymentRepository invoicePaymentRepository,
                         QuoteRepository quoteRepository,
                         RequirementFormRepository requirementFormRepository,
                         LeadService leadService,
                         ProjectService projectService,
                         InvoiceService invoiceService) {
        this.userRepository = userRepository;
        this.leadRepository = leadRepository;
        this.projectRepository = projectRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoicePaymentRepository = invoicePaymentRepository;
        this.quoteRepository = quoteRepository;
        this.requirementFormRepository = requirementFormRepository;
        this.leadService = leadService;
        this.projectService = projectService;
        this.invoiceService = invoiceService;
    }

    public List<ClientDto> list(User current) {
        boolean finance = current.getRole().seesFinance();
        return visibleClients(current).stream().map(client -> toDto(client, finance)).toList();
    }

    public ClientDetailDto get(User current, Long clientId) {
        boolean finance = current.getRole().seesFinance();
        User client = userRepository.findById(clientId)
                .filter(u -> u.getRole() == Role.CUSTOMER)
                .orElseThrow(() -> new NotFoundException("Client not found"));
        if (!current.getRole().isPlatform()
                && visibleClients(current).stream().noneMatch(c -> c.getId().equals(client.getId()))) {
            throw new NotFoundException("Client not found");
        }
        List<Lead> clientLeads = leadRepository.findByCustomerOrderByCreatedAtDesc(client);
        List<Lead> scopedLeads = clientLeads.stream()
                .filter(lead -> leadService.canSee(current, lead))
                .toList();
        List<LeadSummaryDto> leads = scopedLeads.stream().map(leadService::toSummary).toList();
        List<Project> scopedProjects = projectRepository.findByClientOrderByCreatedAtDesc(client)
                .stream()
                .filter(project -> projectService.canSee(current, project))
                .toList();
        List<ProjectDto> projects = scopedProjects.stream()
                .map(project -> projectService.toDto(project, finance, false))
                .toList();
        List<QuoteDto> quotes = null;
        List<InvoiceDto> invoices = null;
        if (finance) {
            quotes = new ArrayList<>();
            for (Lead lead : scopedLeads) {
                quotes.addAll(quoteRepository.findByLeadOrderByVersionDesc(lead)
                        .stream().map(QuoteDto::from).toList());
            }
            invoices = new ArrayList<>();
            for (Project project : scopedProjects) {
                invoices.addAll(invoiceService.forProject(project));
            }
        }
        return new ClientDetailDto(toDto(client, finance), leads, projects, quotes, invoices);
    }

    /**
     * Contact book scope. Derived from {@link LeadService#visibleLeads} rather than
     * rebuilt here: the Leads list and this one are two views of the same book, and when
     * each worked out visibility for itself they disagreed — a consultant who owned a
     * lead as its sales owner (not its designer) watched it leave Leads once the brief
     * landed and never arrive in Customers. One query, one answer, no gap to fall into.
     */
    private List<User> visibleClients(User current) {
        Map<Long, User> clients = new LinkedHashMap<>();
        for (Lead lead : leadService.visibleLeads(current)) {
            if (briefIsIn(lead)) {
                clients.putIfAbsent(lead.getCustomer().getId(), lead.getCustomer());
            }
        }
        // A project you run keeps its client in your book whatever its brief looks like —
        // work that is already under way never drops out of the contact list.
        for (Project project : projectRepository.findByDesignerOrderByCreatedAtDesc(current)) {
            if (project.getClient() != null) {
                clients.putIfAbsent(project.getClient().getId(), project.getClient());
            }
        }
        return new ArrayList<>(clients.values());
    }

    /**
     * The line between a lead and a customer: a design brief that has been submitted,
     * by either side. Until then the person is worked from Leads — a website signup has
     * had an account since the minute they registered, but nothing a designer can work
     * with, and the studio should still be chasing the brief rather than the project.
     */
    private boolean briefIsIn(Lead lead) {
        return lead.getCustomer() != null
                && requirementFormRepository.findByLead(lead)
                        .map(form -> form.getStatus() != RequirementFormStatus.DRAFT)
                        .orElse(false);
    }

    private ClientDto toDto(User client, boolean finance) {
        List<Lead> clientLeads = leadRepository.findByCustomerOrderByCreatedAtDesc(client);
        Lead latest = clientLeads.isEmpty() ? null : clientLeads.get(0);
        String leadStatus = latest == null ? null : latest.getStatus().name();
        List<Project> projects = projectRepository.findByClientOrderByCreatedAtDesc(client);
        BigDecimal lifetimeBilled = null;
        BigDecimal lifetimeCollected = null;
        if (finance) {
            lifetimeBilled = BigDecimal.ZERO;
            lifetimeCollected = BigDecimal.ZERO;
            for (Project project : projects) {
                for (Invoice invoice : invoiceRepository.findByProjectOrderByCreatedAtAsc(project)) {
                    lifetimeBilled = lifetimeBilled.add(InvoiceDto.totalOf(invoice));
                    lifetimeCollected = lifetimeCollected.add(invoicePaymentRepository
                            .findByInvoiceOrderByPaidAtAsc(invoice).stream()
                            .map(p -> p.getAmount())
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
                }
            }
        }
        return new ClientDto(client.getId(), client.getName(), client.getEmail(), client.getPhone(),
                client.getCity(), client.getCreatedAt(), clientLeads.size(), projects.size(),
                latest == null ? null : latest.getId(),
                leadStatus, lifetimeBilled, lifetimeCollected);
    }
}
