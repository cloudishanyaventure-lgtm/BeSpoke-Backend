package com.BeSpoke.service;

import com.BeSpoke.entity.Drawing;
import com.BeSpoke.entity.DrawingStatus;
import com.BeSpoke.entity.Invoice;
import com.BeSpoke.entity.InvoiceStatus;
import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.PlatformOption;
import com.BeSpoke.entity.Project;
import com.BeSpoke.entity.Quote;
import com.BeSpoke.entity.RequirementForm;
import com.BeSpoke.repository.DrawingRepository;
import com.BeSpoke.repository.InvoiceRepository;
import com.BeSpoke.repository.PlatformOptionRepository;
import com.BeSpoke.repository.ProjectRepository;
import com.BeSpoke.repository.RequirementFormRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns work into money owed. The studio agrees a schedule once — 50% on acceptance, 30%
 * at 40% of the work, 20% at 70% — and from then on nobody raises those invoices by hand:
 * accepting the quote raises the advance, and every drawing the customer signs off moves
 * the progress bar and raises whatever the schedule says is now due.
 *
 * <p>Progress is measured the only way the customer would recognise: drawings they have
 * approved, over the spaces in the PRD. A studio with six spaces that has had four
 * drawings signed off is two thirds done, and the money follows that, not an opinion.
 *
 * <p>There is no payment gateway wired in yet, so "payment link" is an invoice in the
 * customer's portal. {@link #raise} is the single seam where a hosted checkout URL goes
 * once there are credentials for one.
 */
// ponytail: percentages live in the PAYMENT_SCHEDULE picklist, not here; a studio that
// wants 40/40/20 edits the list rather than waiting for a deploy.
@Service
public class BillingScheduleService {

    private static final Logger log = LoggerFactory.getLogger(BillingScheduleService.class);

    /** Used only when the picklist has been emptied — the terms the studio started with. */
    private static final List<Instalment> FALLBACK = List.of(
            new Instalment("ADVANCE", "Advance", 50, 0),
            new Instalment("MILESTONE", "Milestone", 30, 40),
            new Instalment("COMPLETION", "Completion", 20, 70));

    /** One line of the payment schedule: what share, and what progress earns it. */
    public record Instalment(String code, String label, int percent, int triggerPct) {
    }

    private final PlatformOptionRepository options;
    private final InvoiceRepository invoiceRepository;
    private final ProjectRepository projectRepository;
    private final DrawingRepository drawingRepository;
    private final RequirementFormRepository requirementFormRepository;
    private final MailService mailService;

    public BillingScheduleService(PlatformOptionRepository options,
                                  InvoiceRepository invoiceRepository,
                                  ProjectRepository projectRepository,
                                  DrawingRepository drawingRepository,
                                  RequirementFormRepository requirementFormRepository,
                                  MailService mailService) {
        this.options = options;
        this.invoiceRepository = invoiceRepository;
        this.projectRepository = projectRepository;
        this.drawingRepository = drawingRepository;
        this.requirementFormRepository = requirementFormRepository;
        this.mailService = mailService;
    }

    /** The agreed terms, in order. */
    public List<Instalment> schedule() {
        List<Instalment> schedule = new ArrayList<>();
        for (PlatformOption option : options.findByListKeyOrderBySortOrderAsc("PAYMENT_SCHEDULE")) {
            String[] parts = (option.getNote() == null ? "" : option.getNote()).split("\\|");
            if (parts.length != 2) {
                continue;
            }
            try {
                schedule.add(new Instalment(option.getValue(), option.getLabel(),
                        Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())));
            } catch (NumberFormatException ex) {
                log.warn("[BILLING] PAYMENT_SCHEDULE row {} has an unreadable note '{}'",
                        option.getValue(), option.getNote());
            }
        }
        return schedule.isEmpty() ? FALLBACK : schedule;
    }

    /**
     * Drawings the customer has signed off, over the spaces in the PRD, as a percentage.
     * Zero when there is nothing to measure against — no PRD means no progress claim.
     */
    public int progressPct(Lead lead) {
        int spaces = requirementFormRepository.findByLead(lead)
                .map(RequirementForm::getRooms).map(List::size).orElse(0);
        if (spaces == 0) {
            return 0;
        }
        long signedOff = drawingRepository.findByLeadOrderByCreatedAtDesc(lead).stream()
                .filter(d -> d.getSupersededAt() == null)
                .filter(d -> d.getStatus() == DrawingStatus.FINAL)
                .map(Drawing::getRequirementRoomId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();
        return (int) Math.min(100, Math.round(signedOff * 100.0 / spaces));
    }

    /** The customer accepted the quote: the advance is due now, before any work starts. */
    @Transactional
    public void onQuoteApproved(Quote quote, BigDecimal quoteTotal) {
        Project project = projectRepository.findByLead(quote.getLead()).orElse(null);
        if (project == null) {
            return;
        }
        project.setBudget(quoteTotal);
        projectRepository.save(project);
        for (Instalment instalment : schedule()) {
            if (instalment.triggerPct() == 0) {
                raise(project, instalment, quoteTotal);
            }
        }
    }

    /**
     * A drawing was signed off — bill whatever the new progress has earned. Runs on every
     * approval and raises nothing it has raised before, so it is safe to call always.
     */
    @Transactional
    public void onProgress(Lead lead) {
        Project project = projectRepository.findByLead(lead).orElse(null);
        if (project == null || project.getBudget() == null) {
            return;
        }
        int progress = progressPct(lead);
        for (Instalment instalment : schedule()) {
            if (instalment.triggerPct() > 0 && progress >= instalment.triggerPct()) {
                raise(project, instalment, project.getBudget());
            }
        }
    }

    /**
     * Raises one instalment, once. The invoice goes out as SENT rather than DRAFT: this is
     * the automatic half of the schedule, and an instalment nobody sent is an instalment
     * nobody pays.
     */
    private void raise(Project project, Instalment instalment, BigDecimal contractValue) {
        if (invoiceRepository.existsByProjectAndScheduleCode(project, instalment.code())) {
            return;
        }
        BigDecimal amount = contractValue
                .multiply(BigDecimal.valueOf(instalment.percent()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        if (amount.signum() <= 0) {
            return;
        }
        Invoice invoice = new Invoice();
        invoice.setProject(project);
        invoice.setScheduleCode(instalment.code());
        invoice.setTitle(instalment.label() + " — " + instalment.percent() + "% of the accepted quote");
        invoice.setAmount(amount);
        invoice.setGstPct(0);
        invoice.setDueDate(LocalDate.now().plusDays(7));
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setNumber("DRAFT-" + java.util.UUID.randomUUID());
        invoice = invoiceRepository.saveAndFlush(invoice);
        invoice.setNumber("INV-" + Year.now() + "-" + String.format("%06d", invoice.getId()));
        invoiceRepository.save(invoice);
        log.info("[BILLING] raised {} ({}%) on project {} — {}",
                instalment.code(), instalment.percent(), project.getId(), invoice.getNumber());
        // ponytail: the customer is told the amount is due and pays out of band; swap this
        // for a hosted checkout link the day there are gateway credentials.
        if (project.getClient() != null) {
            mailService.paymentDue(project.getClient(), invoice.getTitle(), amount);
        }
    }
}
