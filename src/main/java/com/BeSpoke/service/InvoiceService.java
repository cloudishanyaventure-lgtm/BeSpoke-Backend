package com.BeSpoke.service;

import com.BeSpoke.dto.CreateInvoiceRequest;
import com.BeSpoke.dto.InvoiceDto;
import com.BeSpoke.dto.RecordPaymentRequest;
import com.BeSpoke.entity.ActivityType;
import com.BeSpoke.entity.Invoice;
import com.BeSpoke.entity.InvoicePayment;
import com.BeSpoke.entity.InvoiceStatus;
import com.BeSpoke.entity.LeadActivity;
import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.PaymentMode;
import com.BeSpoke.entity.Project;
import com.BeSpoke.entity.ProjectMilestone;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.exception.ConflictException;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.InvoicePaymentRepository;
import com.BeSpoke.repository.InvoiceRepository;
import com.BeSpoke.repository.LeadActivityRepository;
import com.BeSpoke.repository.ProjectMilestoneRepository;
import com.BeSpoke.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMilestoneRepository projectMilestoneRepository;
    private final LeadActivityRepository leadActivityRepository;
    private final MailService mailService;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          InvoicePaymentRepository invoicePaymentRepository,
                          ProjectRepository projectRepository,
                          ProjectMilestoneRepository projectMilestoneRepository,
                          LeadActivityRepository leadActivityRepository,
                          MailService mailService) {
        this.invoiceRepository = invoiceRepository;
        this.invoicePaymentRepository = invoicePaymentRepository;
        this.projectRepository = projectRepository;
        this.projectMilestoneRepository = projectMilestoneRepository;
        this.leadActivityRepository = leadActivityRepository;
        this.mailService = mailService;
    }

    /** Studio scoping: directors touch only their own company's invoices; admins everything. */
    private void checkScope(User actor, Project project) {
        if (actor.getRole().isPlatform()) {
            return;
        }
        Company owner = project.getLead().getCompany();
        if (owner == null || actor.getCompany() == null
                || !owner.getId().equals(actor.getCompany().getId())) {
            throw new NotFoundException("Project not found");
        }
    }

    @Transactional
    public InvoiceDto create(User actor, CreateInvoiceRequest request) {
        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new NotFoundException("Project not found"));
        checkScope(actor, project);
        Invoice invoice = new Invoice();
        invoice.setProject(project);
        if (request.milestoneId() != null) {
            ProjectMilestone milestone = projectMilestoneRepository.findById(request.milestoneId())
                    .orElseThrow(() -> new NotFoundException("Milestone not found"));
            if (!milestone.getProject().getId().equals(project.getId())) {
                throw new BadRequestException("Milestone does not belong to this project");
            }
            invoice.setMilestone(milestone);
        }
        invoice.setNumber("DRAFT-"+java.util.UUID.randomUUID());
        invoice.setTitle(request.title().trim());
        invoice.setAmount(request.amount());
        invoice.setGstPct(request.gstPct());
        invoice.setDueDate(request.dueDate());
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice = invoiceRepository.saveAndFlush(invoice);
        invoice.setNumber("INV-"+java.time.Year.now()+"-"+String.format("%06d",invoice.getId()));
        return toDto(invoice);
    }

    @Transactional
    public InvoiceDto send(User admin, Long invoiceId) {
        invoiceRepository.lockRow(invoiceId).orElseThrow(()->new NotFoundException("Invoice not found"));
        Invoice invoice = requireInvoice(invoiceId);
        checkScope(admin, invoice.getProject());
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new ConflictException("Only DRAFT invoices can be sent");
        }
        invoice.setStatus(InvoiceStatus.SENT);
        invoice = invoiceRepository.save(invoice);
        leadActivityRepository.save(new LeadActivity(invoice.getProject().getLead(), admin,
                ActivityType.SYSTEM, "Invoice " + invoice.getNumber() + " sent"));
        if (invoice.getProject().getClient() != null) {
            // totalOf, not getAmount(): the customer must read the same GST-inclusive
            // figure in the mail that /my/payments shows them.
            mailService.invoiceSent(invoice.getProject().getClient(), invoice.getNumber(),
                    InvoiceDto.totalOf(invoice).toPlainString(), invoice.getDueDate());
        }
        return toDto(invoice);
    }

    @Transactional
    public InvoiceDto recordPayment(User admin, Long invoiceId, RecordPaymentRequest request) {
        invoiceRepository.lockRow(invoiceId).orElseThrow(()->new NotFoundException("Invoice not found"));
        Invoice invoice = requireInvoice(invoiceId);
        checkScope(admin, invoice.getProject());
        if(request.idempotencyKey()!=null&&!request.idempotencyKey().isBlank()){
            var previous=invoicePaymentRepository.findByInvoiceAndIdempotencyKey(invoice,request.idempotencyKey());
            if(previous.isPresent()){
                var p=previous.get();
                if(p.getAmount().compareTo(request.amount())!=0 || !p.getMode().name().equals(request.mode()) || !java.util.Objects.equals(p.getReference(),request.reference()))
                    throw new ConflictException("Payment retry does not match the original request");
                return toDto(invoice);
            }
        }
        BigDecimal existingPaid=invoicePaymentRepository.findByInvoiceOrderByPaidAtAsc(invoice).stream().map(InvoicePayment::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
        if(request.amount().compareTo(InvoiceDto.totalOf(invoice).subtract(existingPaid))>0)throw new BadRequestException("Payment exceeds the outstanding balance");
        if (invoice.getStatus() == InvoiceStatus.DRAFT) {
            throw new ConflictException("Send the invoice before recording payments");
        }
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new ConflictException("Invoice is already fully paid");
        }
        InvoicePayment payment = new InvoicePayment();
        payment.setInvoice(invoice);
        payment.setIdempotencyKey(request.idempotencyKey());
        payment.setAmount(request.amount());
        payment.setMode(PaymentMode.valueOf(request.mode()));
        payment.setReference(request.reference());
        payment.setPaidAt(request.paidAt() != null ? request.paidAt() : LocalDate.now());
        payment.setRecordedBy(admin);
        invoicePaymentRepository.save(payment);

        BigDecimal paid = invoicePaymentRepository.findByInvoiceOrderByPaidAtAsc(invoice).stream()
                .map(InvoicePayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (paid.compareTo(InvoiceDto.totalOf(invoice)) >= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoiceRepository.save(invoice);
        }
        leadActivityRepository.save(new LeadActivity(invoice.getProject().getLead(), admin,
                ActivityType.SYSTEM, "Payment of ₹" + request.amount().toPlainString()
                + " recorded against " + invoice.getNumber()));
        if (invoice.getProject().getClient() != null) {
            mailService.paymentReceived(invoice.getProject().getClient(), invoice.getNumber(),
                    request.amount().toPlainString(),
                    InvoiceDto.totalOf(invoice).subtract(paid).max(BigDecimal.ZERO).toPlainString());
        }
        return toDto(invoice);
    }

    /** status filter accepts derived states too (PARTIALLY_PAID, OVERDUE). */
    public List<InvoiceDto> list(User actor, String status) {
        List<Invoice> invoices;
        if (actor.getRole().isPlatform()) {
            invoices = invoiceRepository.findAllByOrderByCreatedAtDesc();
        } else {
            invoices = actor.getCompany() == null ? List.of()
                    : invoiceRepository.findByProject_Lead_CompanyOrderByCreatedAtDesc(actor.getCompany());
        }
        List<InvoiceDto> dtos = invoices.stream().map(this::toDto).toList();
        if (status == null || status.isBlank()) {
            return dtos;
        }
        String wanted = status.trim().toUpperCase();
        return dtos.stream().filter(dto -> dto.status().equals(wanted)).toList();
    }

    public List<InvoiceDto> forProject(Project project) {
        return invoiceRepository.findByProjectOrderByCreatedAtAsc(project)
                .stream().map(this::toDto).toList();
    }

    public InvoiceDto toDto(Invoice invoice) {
        return InvoiceDto.from(invoice, invoicePaymentRepository.findByInvoiceOrderByPaidAtAsc(invoice));
    }

    private Invoice requireInvoice(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found"));
    }

}
