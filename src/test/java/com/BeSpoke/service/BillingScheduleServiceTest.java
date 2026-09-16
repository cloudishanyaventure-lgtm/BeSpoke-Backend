package com.BeSpoke.service;

import com.BeSpoke.config.SeedRunner;
import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.Drawing;
import com.BeSpoke.entity.DrawingStatus;
import com.BeSpoke.entity.Invoice;
import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.LeadStatus;
import com.BeSpoke.entity.Project;
import com.BeSpoke.entity.ProjectStage;
import com.BeSpoke.entity.Quote;
import com.BeSpoke.entity.RequirementForm;
import com.BeSpoke.entity.RequirementRoom;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.User;
import com.BeSpoke.repository.CompanyRepository;
import com.BeSpoke.repository.DrawingRepository;
import com.BeSpoke.repository.InvoiceRepository;
import com.BeSpoke.repository.LeadRepository;
import com.BeSpoke.repository.ProjectRepository;
import com.BeSpoke.repository.QuoteRepository;
import com.BeSpoke.repository.RequirementFormRepository;
import com.BeSpoke.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The money follows the work. A studio that agreed 50/30/20 should never raise those
 * invoices by hand, and should never raise one twice however many times a drawing is
 * signed off.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:billing;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "app.mail.enabled=false",
        "app.inbox.enabled=false", "spring.jpa.show-sql=false"
})
@Transactional
class BillingScheduleServiceTest {

    @MockBean SeedRunner seedRunner;
    @Autowired BillingScheduleService billing;
    @Autowired CompanyRepository companies;
    @Autowired UserRepository users;
    @Autowired LeadRepository leads;
    @Autowired ProjectRepository projects;
    @Autowired InvoiceRepository invoices;
    @Autowired DrawingRepository drawings;
    @Autowired QuoteRepository quotes;
    @Autowired RequirementFormRepository forms;

    Lead lead;
    Project project;
    RequirementForm form;

    @BeforeEach void setup() {
        Company studio = companies.save(new Company("Billing studio", "billing-studio"));
        User customer = users.save(new User("Bill Payer", "payer@billing.test", "unused", Role.CUSTOMER));
        lead = new Lead();
        lead.setContactName("Bill Payer");
        lead.setContactEmail("payer@billing.test");
        lead.setContactPhone("9800000100");
        lead.setCity("Pune");
        lead.setSource(com.BeSpoke.entity.LeadSource.WALK_IN);
        lead.setCompany(studio);
        lead.setCustomer(customer);
        lead.setStatus(LeadStatus.WON);
        lead = leads.save(lead);

        project = new Project();
        project.setLead(lead);
        project.setClient(customer);
        project.setName("Bill Payer — Apartment");
        project.setStage(ProjectStage.DESIGN_BRIEF);
        project = projects.save(project);

        // Four spaces: each one signed off is 25% of the job.
        form = new RequirementForm();
        form.setLead(lead);
        form = forms.save(form);
        for (String label : List.of("Kitchen", "Living room", "Master bedroom", "Study")) {
            RequirementRoom room = new RequirementRoom();
            room.setForm(form);
            room.setRoomType(label.toUpperCase().replace(' ', '_'));
            room.setLabel(label);
            form.getRooms().add(room);
        }
        form = forms.save(form);
    }

    @Test void acceptingTheQuoteRaisesTheAdvanceAndNothingElse() {
        approveQuote(new BigDecimal("200000"));

        List<Invoice> raised = invoices.findByProjectOrderByCreatedAtAsc(project);
        assertEquals(1, raised.size(), "only the advance is due on day one");
        assertEquals(0, new BigDecimal("100000.00").compareTo(raised.get(0).getAmount()),
                "50% of the accepted quote");
        assertEquals("ADVANCE", raised.get(0).getScheduleCode());
    }

    @Test void theRestFollowsTheWorkTheCustomerHasSignedOff() {
        approveQuote(new BigDecimal("200000"));

        // One space of four signed off — 25%, short of the 40% the milestone asks for.
        signOff(0);
        billing.onProgress(lead);
        assertEquals(25, billing.progressPct(lead));
        assertEquals(1, invoices.findByProjectOrderByCreatedAtAsc(project).size());

        // Two of four is 50%: the 30% milestone is earned.
        signOff(1);
        billing.onProgress(lead);
        assertEquals(50, billing.progressPct(lead));
        assertEquals(0, new BigDecimal("60000.00").compareTo(amountOf("MILESTONE")));

        // Three of four is 75%, past 70%: the balance falls due and the job is fully billed.
        signOff(2);
        billing.onProgress(lead);
        assertEquals(75, billing.progressPct(lead));
        assertEquals(0, new BigDecimal("40000.00").compareTo(amountOf("COMPLETION")));
        assertEquals(0, new BigDecimal("200000.00").compareTo(billedTotal()),
                "the whole contract is billed by 70% of the work");
    }

    @Test void anInstalmentIsNeverRaisedTwice() {
        approveQuote(new BigDecimal("200000"));
        signOff(0);
        signOff(1);
        for (int i = 0; i < 5; i++) {
            billing.onProgress(lead);
        }
        assertEquals(2, invoices.findByProjectOrderByCreatedAtAsc(project).size(),
                "five sign-offs, two instalments");
    }

    @Test void nothingIsBilledWithoutAnAcceptedQuote() {
        signOff(0);
        signOff(1);
        billing.onProgress(lead);
        assertTrue(invoices.findByProjectOrderByCreatedAtAsc(project).isEmpty(),
                "no accepted value, nothing to take a percentage of");
    }

    private void approveQuote(BigDecimal total) {
        Quote quote = new Quote();
        quote.setLead(lead);
        quote.setVersion(1);
        quote.setTitle("Interior design proposal");
        quotes.save(quote);
        billing.onQuoteApproved(quote, total);
    }

    /** The customer signing off the drawing for one space — the only thing that counts. */
    private void signOff(int roomIndex) {
        Drawing drawing = new Drawing();
        drawing.setLead(lead);
        drawing.setTitle("Drawing " + roomIndex);
        drawing.setFileUrl("https://example.test/d" + roomIndex + ".pdf");
        drawing.setStatus(DrawingStatus.FINAL);
        drawing.setRequirementRoomId(form.getRooms().get(roomIndex).getId());
        drawings.save(drawing);
    }

    private BigDecimal amountOf(String scheduleCode) {
        return invoices.findByProjectOrderByCreatedAtAsc(project).stream()
                .filter(i -> scheduleCode.equals(i.getScheduleCode()))
                .map(Invoice::getAmount)
                .findFirst()
                .orElseThrow(() -> new AssertionError(scheduleCode + " was never raised"));
    }

    private BigDecimal billedTotal() {
        return invoices.findByProjectOrderByCreatedAtAsc(project).stream()
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
