package com.BeSpoke.controller;

import com.BeSpoke.dto.ActivityDto;
import com.BeSpoke.dto.InvoiceDto;
import com.BeSpoke.dto.JourneyDto;
import com.BeSpoke.dto.MessageDto;
import com.BeSpoke.dto.QuoteDecisionRequest;
import com.BeSpoke.dto.QuoteDto;
import com.BeSpoke.dto.RequirementFormDto;
import com.BeSpoke.dto.RequirementFormRequest;
import com.BeSpoke.dto.RoomRequest;
import com.BeSpoke.dto.SendMessageRequest;
import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.Project;
import com.BeSpoke.entity.User;
import com.BeSpoke.repository.ProjectRepository;
import com.BeSpoke.service.CurrentUserService;
import com.BeSpoke.service.InvoiceService;
import com.BeSpoke.service.JourneyService;
import com.BeSpoke.service.MessageService;
import com.BeSpoke.service.QuoteService;
import com.BeSpoke.service.RequirementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Customer portal - always scoped to the authenticated customer's own lead. */
@RestController
@RequestMapping("/api/my")
public class MyController {

    private final CurrentUserService currentUserService;
    private final JourneyService journeyService;
    private final RequirementService requirementService;
    private final QuoteService quoteService;
    private final InvoiceService invoiceService;
    private final MessageService messageService;
    private final ProjectRepository projectRepository;

    public MyController(CurrentUserService currentUserService,
                        JourneyService journeyService,
                        RequirementService requirementService,
                        QuoteService quoteService,
                        InvoiceService invoiceService,
                        MessageService messageService,
                        ProjectRepository projectRepository) {
        this.currentUserService = currentUserService;
        this.journeyService = journeyService;
        this.requirementService = requirementService;
        this.quoteService = quoteService;
        this.invoiceService = invoiceService;
        this.messageService = messageService;
        this.projectRepository = projectRepository;
    }

    private User me(Authentication authentication) {
        return currentUserService.requireByEmail(authentication.getName());
    }

    @GetMapping("/journey")
    public JourneyDto journey(Authentication authentication) {
        return journeyService.journey(me(authentication));
    }

    @GetMapping("/requirement-form")
    public RequirementFormDto form(Authentication authentication) {
        return requirementService.myForm(me(authentication));
    }

    @PutMapping("/requirement-form")
    public RequirementFormDto upsertForm(Authentication authentication,
                                         @Valid @RequestBody RequirementFormRequest request) {
        return requirementService.upsert(me(authentication), request);
    }

    @PutMapping("/requirement-form/rooms")
    public RequirementFormDto replaceRooms(Authentication authentication,
                                           @Valid @RequestBody List<RoomRequest> rooms) {
        return requirementService.replaceRooms(me(authentication), rooms);
    }

    @PostMapping("/requirement-form/submit")
    public RequirementFormDto submitForm(Authentication authentication) {
        return requirementService.submit(me(authentication));
    }

    /** Customer sign-off on the submitted brief — locks it for further customer edits. */
    @PostMapping("/requirement-form/approve")
    public RequirementFormDto approveForm(Authentication authentication) {
        return requirementService.approve(me(authentication));
    }

    @GetMapping("/requirement-form/activities")
    public List<ActivityDto> formActivities(Authentication authentication) {
        return requirementService.myActivities(me(authentication));
    }

    @GetMapping("/quotes")
    public List<QuoteDto> quotes(Authentication authentication) {
        User customer = me(authentication);
        return quoteService.myQuotes(customer, requirementService.myLead(customer));
    }

    @PostMapping("/quotes/{id}/decision")
    public QuoteDto decide(Authentication authentication,
                           @PathVariable Long id,
                           @Valid @RequestBody QuoteDecisionRequest request) {
        User customer = me(authentication);
        return quoteService.decide(customer, requirementService.myLead(customer), id, request);
    }

    @GetMapping("/invoices")
    public List<InvoiceDto> invoices(Authentication authentication) {
        User customer = me(authentication);
        Lead lead = requirementService.myLead(customer);
        Project project = projectRepository.findByLead(lead).orElse(null);
        if (project == null) {
            return List.of();
        }
        // Customers never see internal drafts.
        return invoiceService.forProject(project).stream()
                .filter(invoice -> !"DRAFT".equals(invoice.status()))
                .toList();
    }

    @GetMapping("/messages")
    public List<MessageDto> messages(Authentication authentication) {
        User customer = me(authentication);
        return messageService.messagesFor(requirementService.myLead(customer), customer);
    }

    @PostMapping("/messages")
    public ResponseEntity<MessageDto> sendMessage(Authentication authentication,
                                                  @Valid @RequestBody SendMessageRequest request) {
        User customer = me(authentication);
        MessageDto message = messageService.send(
                requirementService.myLead(customer), customer, request.body());
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }
}
