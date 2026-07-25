package com.BeSpoke.service;

import com.BeSpoke.dto.CreateQuoteRequest;
import com.BeSpoke.dto.QuoteDecisionRequest;
import com.BeSpoke.dto.QuoteDto;
import com.BeSpoke.dto.QuoteItemRequest;
import com.BeSpoke.dto.UpdateQuoteRequest;
import com.BeSpoke.entity.ActivityType;
import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.LeadActivity;
import com.BeSpoke.entity.Quote;
import com.BeSpoke.entity.QuoteItem;
import com.BeSpoke.entity.QuoteItemCategory;
import com.BeSpoke.entity.QuoteStatus;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.exception.ConflictException;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.LeadActivityRepository;
import com.BeSpoke.repository.LeadRepository;
import com.BeSpoke.repository.QuoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class QuoteService {

    /** Statuses a customer may see (everything except staff drafts). */
    private static final List<QuoteStatus> CUSTOMER_VISIBLE = List.of(
            QuoteStatus.SENT, QuoteStatus.APPROVED, QuoteStatus.CHANGES_REQUESTED);

    private final QuoteRepository quoteRepository;
    private final LeadRepository leadRepository;
    private final LeadActivityRepository leadActivityRepository;

    public QuoteService(QuoteRepository quoteRepository,
                        LeadRepository leadRepository,
                        LeadActivityRepository leadActivityRepository) {
        this.quoteRepository = quoteRepository;
        this.leadRepository = leadRepository;
        this.leadActivityRepository = leadActivityRepository;
    }

    @Transactional
    public QuoteDto create(User admin, CreateQuoteRequest request) {
        Lead lead = leadRepository.findById(request.leadId())
                .orElseThrow(() -> new NotFoundException("Lead not found"));
        int version = quoteRepository.findFirstByLeadOrderByVersionDesc(lead)
                .map(q -> q.getVersion() + 1).orElse(1);
        Quote quote = new Quote();
        quote.setLead(lead);
        quote.setVersion(version);
        quote.setTitle(request.title().trim());
        quote.setValidUntil(request.validUntil());
        quote.setStatus(QuoteStatus.DRAFT);
        applyItems(quote, request.items());
        quote = quoteRepository.save(quote);
        return QuoteDto.from(quote);
    }

    @Transactional
    public QuoteDto update(Long quoteId, UpdateQuoteRequest request) {
        Quote quote = requireQuote(quoteId);
        if (quote.getStatus() != QuoteStatus.DRAFT) {
            throw new ConflictException("Only DRAFT quotes can be edited — use revise to create a new version");
        }
        quote.setTitle(request.title().trim());
        quote.setValidUntil(request.validUntil());
        applyItems(quote, request.items());
        return QuoteDto.from(quoteRepository.save(quote));
    }

    @Transactional
    public QuoteDto send(User admin, Long quoteId) {
        Quote quote = requireQuote(quoteId);
        if (quote.getStatus() != QuoteStatus.DRAFT) {
            throw new ConflictException("Only DRAFT quotes can be sent");
        }
        quote.setStatus(QuoteStatus.SENT);
        quote.setSentAt(Instant.now());
        quote = quoteRepository.save(quote);
        leadActivityRepository.save(new LeadActivity(quote.getLead(), admin, ActivityType.SYSTEM,
                "Quote v" + quote.getVersion() + " \"" + quote.getTitle() + "\" sent to customer"));
        return QuoteDto.from(quote);
    }

    /** Copies the quote into a fresh DRAFT with version+1. */
    @Transactional
    public QuoteDto revise(User admin, Long quoteId) {
        Quote source = requireQuote(quoteId);
        int version = quoteRepository.findFirstByLeadOrderByVersionDesc(source.getLead())
                .map(q -> q.getVersion() + 1).orElse(source.getVersion() + 1);
        Quote copy = new Quote();
        copy.setLead(source.getLead());
        copy.setVersion(version);
        copy.setTitle(source.getTitle());
        copy.setValidUntil(source.getValidUntil());
        copy.setStatus(QuoteStatus.DRAFT);
        for (QuoteItem item : source.getItems()) {
            copy.getItems().add(new QuoteItem(copy, item.getCategory(), item.getDescription(),
                    item.getQty(), item.getRate(), item.getGstPct()));
        }
        copy = quoteRepository.save(copy);
        leadActivityRepository.save(new LeadActivity(copy.getLead(), admin, ActivityType.SYSTEM,
                "Quote revised — v" + version + " drafted from v" + source.getVersion()));
        return QuoteDto.from(copy);
    }

    public List<QuoteDto> list(String status) {
        List<Quote> quotes;
        if (status == null || status.isBlank()) {
            quotes = quoteRepository.findAllByOrderByCreatedAtDesc();
        } else {
            QuoteStatus parsed;
            try {
                parsed = QuoteStatus.valueOf(status.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Unknown quote status: " + status);
            }
            quotes = quoteRepository.findByStatusOrderByCreatedAtDesc(parsed);
        }
        return quotes.stream().map(QuoteDto::from).toList();
    }

    public QuoteDto get(Long quoteId) {
        return QuoteDto.from(requireQuote(quoteId));
    }

    /** Customer view: quotes on their lead, drafts hidden. */
    public List<QuoteDto> myQuotes(User customer, Lead lead) {
        return quoteRepository.findByLeadAndStatusInOrderByVersionDesc(lead, CUSTOMER_VISIBLE)
                .stream().map(QuoteDto::from).toList();
    }

    @Transactional
    public QuoteDto decide(User customer, Lead lead, Long quoteId, QuoteDecisionRequest request) {
        Quote quote = requireQuote(quoteId);
        if (!quote.getLead().getId().equals(lead.getId())) {
            throw new NotFoundException("Quote not found");
        }
        if (quote.getStatus() != QuoteStatus.SENT) {
            throw new ConflictException("This quote is not awaiting a decision");
        }
        quote.setDecidedAt(Instant.now());
        if ("APPROVED".equals(request.decision())) {
            quote.setStatus(QuoteStatus.APPROVED);
            leadActivityRepository.save(new LeadActivity(lead, customer, ActivityType.SYSTEM,
                    "Quote v" + quote.getVersion() + " approved by customer"));
        } else {
            quote.setStatus(QuoteStatus.CHANGES_REQUESTED);
            quote.setCustomerComment(request.comment());
            leadActivityRepository.save(new LeadActivity(lead, customer, ActivityType.SYSTEM,
                    "Customer requested changes on quote v" + quote.getVersion()
                            + (request.comment() != null && !request.comment().isBlank()
                               ? ": " + request.comment().trim() : "")));
        }
        return QuoteDto.from(quoteRepository.save(quote));
    }

    private Quote requireQuote(Long quoteId) {
        return quoteRepository.findById(quoteId)
                .orElseThrow(() -> new NotFoundException("Quote not found"));
    }

    private void applyItems(Quote quote, List<QuoteItemRequest> items) {
        quote.getItems().clear();
        for (QuoteItemRequest item : items) {
            quote.getItems().add(new QuoteItem(quote, QuoteItemCategory.valueOf(item.category()),
                    item.description().trim(), item.qty(), item.rate(), item.gstPct()));
        }
    }
}
