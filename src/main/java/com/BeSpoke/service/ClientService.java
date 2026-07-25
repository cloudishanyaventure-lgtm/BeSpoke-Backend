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
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.InvoicePaymentRepository;
import com.BeSpoke.repository.InvoiceRepository;
import com.BeSpoke.repository.LeadRepository;
import com.BeSpoke.repository.ProjectRepository;
import com.BeSpoke.repository.QuoteRepository;
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
    private final LeadService leadService;
    private final ProjectService projectService;
    private final InvoiceService invoiceService;

    public ClientService(UserRepository userRepository,
                         LeadRepository leadRepository,
                         ProjectRepository projectRepository,
                         InvoiceRepository invoiceRepository,
                         InvoicePaymentRepository invoicePaymentRepository,
                         QuoteRepository quoteRepository,
                         LeadService leadService,
                         ProjectService projectService,
                         InvoiceService invoiceService) {
        this.userRepository = userRepository;
        this.leadRepository = leadRepository;
        this.projectRepository = projectRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoicePaymentRepository = invoicePaymentRepository;
        this.quoteRepository = quoteRepository;
        this.leadService = leadService;
        this.projectService = projectService;
        this.invoiceService = invoiceService;
    }

    public List<ClientDto> list(User current) {
        boolean admin = current.getRole() == Role.ADMIN;
        List<User> clients = admin ? allCustomers() : designerClients(current);
        return clients.stream().map(client -> toDto(client, admin)).toList();
    }

    public ClientDetailDto get(User current, Long clientId) {
        boolean admin = current.getRole() == Role.ADMIN;
        User client = userRepository.findById(clientId)
                .filter(u -> u.getRole() == Role.CUSTOMER)
                .orElseThrow(() -> new NotFoundException("Client not found"));
        if (!admin && designerClients(current).stream().noneMatch(c -> c.getId().equals(client.getId()))) {
            throw new NotFoundException("Client not found");
        }
        List<Lead> clientLeads = leadRepository.findByCustomerOrderByCreatedAtDesc(client);
        List<LeadSummaryDto> leads = clientLeads.stream()
                .filter(lead -> admin || isAssignedTo(lead, current))
                .map(leadService::toSummary)
                .toList();
        List<ProjectDto> projects = projectRepository.findByClientOrderByCreatedAtDesc(client)
                .stream()
                .filter(project -> admin
                        || (project.getDesigner() != null && project.getDesigner().getId().equals(current.getId())))
                .map(project -> projectService.toDto(project, admin, false))
                .toList();
        List<QuoteDto> quotes = null;
        List<InvoiceDto> invoices = null;
        if (admin) {
            quotes = new ArrayList<>();
            for (Lead lead : clientLeads) {
                quotes.addAll(quoteRepository.findByLeadOrderByVersionDesc(lead)
                        .stream().map(QuoteDto::from).toList());
            }
            invoices = new ArrayList<>();
            for (Project project : projectRepository.findByClientOrderByCreatedAtDesc(client)) {
                invoices.addAll(invoiceService.forProject(project));
            }
        }
        return new ClientDetailDto(toDto(client, admin), leads, projects, quotes, invoices);
    }

    private boolean isAssignedTo(Lead lead, User designer) {
        return lead.getAssignedDesigner() != null
                && lead.getAssignedDesigner().getId().equals(designer.getId());
    }

    private List<User> allCustomers() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(user -> user.getRole() == Role.CUSTOMER)
                .toList();
    }

    /** Customers a designer serves: owners of their assigned leads or of projects they run. */
    private List<User> designerClients(User designer) {
        // Key by id: User has no equals/hashCode override and instances come from separate queries.
        Map<Long, User> clients = new LinkedHashMap<>();
        for (Lead lead : leadRepository.findByAssignedDesignerOrderByCreatedAtDesc(designer)) {
            if (lead.getCustomer() != null) {
                clients.putIfAbsent(lead.getCustomer().getId(), lead.getCustomer());
            }
        }
        for (Project project : projectRepository.findByDesignerOrderByCreatedAtDesc(designer)) {
            if (project.getClient() != null) {
                clients.putIfAbsent(project.getClient().getId(), project.getClient());
            }
        }
        return new ArrayList<>(clients.values());
    }

    private ClientDto toDto(User client, boolean admin) {
        List<Lead> clientLeads = leadRepository.findByCustomerOrderByCreatedAtDesc(client);
        String leadStatus = clientLeads.isEmpty() ? null : clientLeads.get(0).getStatus().name();
        List<Project> projects = projectRepository.findByClientOrderByCreatedAtDesc(client);
        BigDecimal lifetimeBilled = null;
        BigDecimal lifetimeCollected = null;
        if (admin) {
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
                leadStatus, lifetimeBilled, lifetimeCollected);
    }
}
