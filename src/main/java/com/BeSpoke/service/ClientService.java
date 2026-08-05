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

    /** Contact book scope: admin all, company-wide roles their studio's customers, designers their own. */
    private List<User> visibleClients(User current) {
        if (current.getRole().isPlatform()) {
            return allCustomers();
        }
        if (current.getRole().seesWholeCompany()) {
            if (current.getCompany() == null) {
                return List.of();
            }
            Map<Long, User> clients = new LinkedHashMap<>();
            for (Lead lead : leadRepository.findByCompanyOrderByCreatedAtDesc(current.getCompany())) {
                if (lead.getCustomer() != null) {
                    clients.putIfAbsent(lead.getCustomer().getId(), lead.getCustomer());
                }
            }
            return new ArrayList<>(clients.values());
        }
        return designerClients(current);
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

    private ClientDto toDto(User client, boolean finance) {
        List<Lead> clientLeads = leadRepository.findByCustomerOrderByCreatedAtDesc(client);
        String leadStatus = clientLeads.isEmpty() ? null : clientLeads.get(0).getStatus().name();
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
                leadStatus, lifetimeBilled, lifetimeCollected);
    }
}
