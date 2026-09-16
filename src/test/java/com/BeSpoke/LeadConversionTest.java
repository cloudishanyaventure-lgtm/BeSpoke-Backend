package com.BeSpoke;

import com.BeSpoke.config.SeedRunner;
import com.BeSpoke.dto.CreateLeadRequest;
import com.BeSpoke.dto.LeadSummaryDto;
import com.BeSpoke.dto.StageChangeRequest;
import com.BeSpoke.dto.RequirementFormRequest;
import com.BeSpoke.dto.RoomRequest;
import com.BeSpoke.dto.UpdateLeadContactRequest;
import com.BeSpoke.entity.*;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.exception.ConflictException;
import com.BeSpoke.repository.*;
import com.BeSpoke.service.ClientService;
import com.BeSpoke.service.LeadService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** A worked lead is a customer: CONTACTED mints the account the contact book and portal need. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:leadconversion;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.mail.enabled=false", "app.inbox.enabled=false", "spring.jpa.show-sql=false"
})
@Transactional
class LeadConversionTest {
    @MockBean SeedRunner seedRunner;
    @Autowired LeadService leadService;
    @Autowired ClientService clientService;
    @Autowired LeadRepository leads;
    @Autowired UserRepository users;
    @Autowired CompanyRepository companies;
    @Autowired RequirementFormRepository forms;
    @Autowired com.BeSpoke.service.RequirementService requirementService;

    User director, admin;
    Company studio;

    @BeforeEach void setup() {
        studio = new Company("Conversion studio", "conversion-studio");
        studio.setKycStatus(KycStatus.VERIFIED);   // so the platform may route pool leads to it
        studio = companies.save(studio);
        director = new User("Director", "director@conversion.test", "unused", Role.DIRECTOR);
        director.setCompany(studio);
        director = users.save(director);
        admin = users.save(new User("Platform", "platform@conversion.test", "unused", Role.ADMIN));
    }

    LeadSummaryDto capture(String name, String email, String phone) {
        return leadService.createManual(director, new CreateLeadRequest(
                name, phone, email, "Pune", "APARTMENT", "B10_20L", "WALK_IN"));
    }

    void contact(Long leadId) {
        captureBrief(leadId);  // a lead only reaches CONTACTED once its design brief exists
        leadService.changeStage(director, leadId, new StageChangeRequest("CONTACTED", null));
    }

    /** The studio taking the brief down over the phone, which is what unlocks CONTACTED. */
    void captureBrief(Long leadId) {
        Lead lead = leads.findById(leadId).orElseThrow();
        if (forms.findByLead(lead).isPresent()) return;
        RequirementForm form = new RequirementForm();
        form.setLead(lead);
        form.setStatus(RequirementFormStatus.SUBMITTED);
        forms.save(form);
    }

    /** The same capture, left as a draft so staffSubmit is the thing that completes it. */
    void captureDraftBrief(Long leadId) {
        RequirementForm form = new RequirementForm();
        form.setLead(leads.findById(leadId).orElseThrow());
        form.setStatus(RequirementFormStatus.DRAFT);
        forms.save(form);
    }

    @Test void completingTheBriefIsItselfTheConversion() {
        LeadSummaryDto captured = capture("Brief Only", "briefonly@home.test", "9800000011");
        assertTrue(clientService.list(director).isEmpty(), "a captured lead is nobody's customer yet");

        // No stage move at all — just the studio marking the design brief complete.
        captureDraftBrief(captured.id());
        requirementService.staffSubmit(leads.findById(captured.id()).orElseThrow(), director);

        Lead lead = leads.findById(captured.id()).orElseThrow();
        assertNotNull(lead.getCustomer(), "a completed brief converts the lead");
        assertEquals("briefonly@home.test", lead.getCustomer().getEmail());
        assertEquals(LeadStatus.NEW_INQUIRY, lead.getStatus(), "and leaves the funnel alone");
        assertEquals(1, clientService.list(director).size(), "they are in Customers now");
    }

    /**
     * The two documents lock at different moments: the brief is phase one and freezes on
     * the studio's sign-off, the PRD is phase two and is filled from that point onwards.
     * Locking used to freeze both, which left a designer with a locked brief, an empty
     * PRD and no button to press.
     */
    @Test void lockingTheBriefFreezesItAndOpensThePrd() {
        LeadSummaryDto captured = capture("Locked Brief", "lockedbrief@home.test", "9800000014");
        captureDraftBrief(captured.id());
        requirementService.staffSubmit(leads.findById(captured.id()).orElseThrow(), director);
        requirementService.studioApprove(leads.findById(captured.id()).orElseThrow(), director);
        assertEquals(RequirementFormStatus.LOCKED,
                forms.findByLead(leads.findById(captured.id()).orElseThrow()).orElseThrow().getStatus(),
                "locking an empty PRD is the normal flow, not an error");

        // Phase two: the detail goes in space by space, with the brief already locked.
        requirementService.staffReplaceRooms(leads.findById(captured.id()).orElseThrow(),
                List.of(new RoomRequest("KITCHEN", "Kitchen", "Ground floor", null, null,
                        null, null, null, null, null, null,
                        9.0, 12.0, 10.0, 0.0,
                        List.of(new RoomRequest.RoomItemRequest("Storage", "Base units", null)))));
        RequirementForm form = forms.findByLead(leads.findById(captured.id()).orElseThrow()).orElseThrow();
        assertEquals(1, form.getRooms().size(), "the PRD is still writable after the lock");
        assertEquals(1, form.getRooms().get(0).getItems().size());
        // Depth was answered 0 — "not applicable" — so it is left out rather than
        // multiplying the room's area down to nothing.
        assertEquals(120.0, form.getRooms().get(0).areaSqft(), 0.001);

        // The brief itself is what the lock froze.
        Lead lead = leads.findById(captured.id()).orElseThrow();
        assertThrows(ConflictException.class,
                () -> requirementService.staffUpsertForm(lead, blankBrief()),
                "the brief stays frozen");

        requirementService.studioReopen(leads.findById(captured.id()).orElseThrow(), director);
        assertEquals(RequirementFormStatus.SUBMITTED,
                forms.findByLead(leads.findById(captured.id()).orElseThrow()).orElseThrow().getStatus(),
                "and a mistaken lock can be taken back off the shelf");
    }

    /** Every scalar null — enough to prove the write is refused, not that it writes well. */
    RequirementFormRequest blankBrief() {
        Object[] args = new Object[RequirementFormRequest.class.getRecordComponents().length];
        try {
            var ctor = RequirementFormRequest.class.getDeclaredConstructors()[0];
            return (RequirementFormRequest) ctor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test void whoeverCanSeeTheLeadSeesTheCustomerItBecomes() {
        // The consultant owns this lead as its sales owner and is not its designer. Both
        // lists are the same book: a lead that leaves theirs has to arrive in the other.
        User consultant = new User("Puneet", "consultant@conversion.test", "unused", Role.CUSTOMER_CONSULTANT);
        consultant.setCompany(studio);
        consultant = users.save(consultant);
        User designer = new User("Laxmi", "designer@conversion.test", "unused", Role.DESIGNER);
        designer.setCompany(studio);
        designer = users.save(designer);

        LeadSummaryDto captured = capture("Kapil Agarwal", "kapil@home.test", "9800000013");
        Lead lead = leads.findById(captured.id()).orElseThrow();
        lead.setSalesOwner(consultant);
        lead.setAssignedDesigner(designer);
        leads.save(lead);
        assertTrue(leadService.list(consultant, null, null, null).stream()
                .anyMatch(l -> l.id().equals(captured.id())), "the consultant works this lead");

        captureDraftBrief(captured.id());
        requirementService.staffSubmit(leads.findById(captured.id()).orElseThrow(), director);

        assertEquals(1, clientService.list(consultant).size(),
                "so the customer it became is in their contact book too");
        assertEquals(1, clientService.list(designer).size(), "and in the designer's");
        assertEquals(1, clientService.list(director).size(), "and the director's");
    }

    @Test void aSignedUpCustomerIsStillALeadUntilTheirBriefIsIn() {
        // A website signup owns an account from the minute they register — but with no
        // brief there is nothing to design, so they belong to Leads, not Customers.
        LeadSummaryDto captured = capture("Signed Up", "signedup@home.test", "9800000012");
        Lead lead = leads.findById(captured.id()).orElseThrow();
        lead.setCustomer(users.save(new User("Signed Up", "signedup@home.test", "unused", Role.CUSTOMER)));
        leads.save(lead);
        assertTrue(clientService.list(director).isEmpty(), "an account alone is not a customer");

        captureDraftBrief(captured.id());
        assertTrue(clientService.list(director).isEmpty(), "and neither is a half-typed draft");

        requirementService.staffSubmit(leads.findById(captured.id()).orElseThrow(), director);
        assertEquals(1, clientService.list(director).size(), "the submitted brief is the line");
    }

    @Test void aLeadWithNoDesignBriefCannotBeMarkedContacted() {
        LeadSummaryDto captured = capture("No Brief", "nobrief@home.test", "9800000009");

        BadRequestException blocked = assertThrows(BadRequestException.class, () ->
                leadService.changeStage(director, captured.id(), new StageChangeRequest("CONTACTED", null)));
        assertTrue(blocked.getMessage().contains("design brief"), blocked.getMessage());
        assertNull(leads.findById(captured.id()).orElseThrow().getCustomer(),
                "and nothing was converted on the way out");

        contact(captured.id());
        assertEquals(LeadStatus.CONTACTED, leads.findById(captured.id()).orElseThrow().getStatus());
    }

    @Test void aProposalNeedsAPrdTheCustomerHasApproved() {
        LeadSummaryDto captured = capture("Needs Prd", "needsprd@home.test", "9800000010");
        contact(captured.id());

        BadRequestException noRooms = assertThrows(BadRequestException.class, () ->
                leadService.changeStage(director, captured.id(), new StageChangeRequest("PROPOSAL_SENT", null)));
        assertTrue(noRooms.getMessage().contains("PRD"), noRooms.getMessage());

        Lead lead = leads.findById(captured.id()).orElseThrow();
        RequirementForm form = forms.findByLead(lead).orElseThrow();
        RequirementRoom kitchen = new RequirementRoom();
        kitchen.setForm(form);
        kitchen.setRoomType("KITCHEN");
        kitchen.setLabel("Kitchen");
        form.getRooms().add(kitchen);
        forms.save(form);

        BadRequestException notApproved = assertThrows(BadRequestException.class, () ->
                leadService.changeStage(director, captured.id(), new StageChangeRequest("PROPOSAL_SENT", null)));
        assertTrue(notApproved.getMessage().contains("approved"), notApproved.getMessage());

        form.setStatus(RequirementFormStatus.APPROVED);
        forms.save(form);
        leadService.changeStage(director, captured.id(), new StageChangeRequest("PROPOSAL_SENT", null));
        assertEquals(LeadStatus.PROPOSAL_SENT, leads.findById(captured.id()).orElseThrow().getStatus());
    }

    @Test void contactingAWalkInLeadCreatesTheCustomerAndListsThemAsAContact() {
        LeadSummaryDto captured = capture("Riya Shah", "riya@home.test", "9800000001");
        assertFalse(captured.hasCustomerAccount(), "a fresh manual lead has no account");
        assertTrue(clientService.list(director).isEmpty(), "and is not in the contact book yet");

        contact(captured.id());

        Lead lead = leads.findById(captured.id()).orElseThrow();
        User customer = lead.getCustomer();
        assertNotNull(customer, "CONTACTED converts the lead");
        assertEquals(Role.CUSTOMER, customer.getRole());
        assertEquals("riya@home.test", customer.getEmail());
        assertEquals("9800000001", customer.getPhone());
        assertEquals("Pune", customer.getCity());
        assertEquals(1, clientService.list(director).size(), "and it shows in Contacts");
        assertEquals(customer.getId(), clientService.list(director).get(0).userId());
        // The requirement travels with the customer, so the contact reads as a customer record.
        assertEquals(captured.id(), clientService.get(director, customer.getId()).leads().get(0).id());
    }

    @Test void asecondLeadForTheSamePersonReusesTheOneAccount() {
        LeadSummaryDto first = capture("Riya Shah", "riya@home.test", "9800000001");
        contact(first.id());
        Long customerId = leads.findById(first.id()).orElseThrow().getCustomer().getId();

        LeadSummaryDto second = capture("Riya Shah", "riya@home.test", "9800000002");
        contact(second.id());

        assertEquals(customerId, leads.findById(second.id()).orElseThrow().getCustomer().getId());
        assertEquals(1, clientService.list(director).size());
    }

    @Test void staffCanCompleteTheProfileTheCustomerNeverGave() {
        // An email enquiry arrives with no phone and no city — the consultant collects them.
        Lead fromEmail = leadService.createFromEmail("nikhil@home.test", "Nikhil", "Kitchen redo", "Hi");
        assertEquals("", fromEmail.getContactPhone());
        leadService.route(admin, fromEmail.getId(), studio.getId());
        leadService.accept(director, fromEmail.getId());
        contact(fromEmail.getId());

        leadService.updateContact(director, fromEmail.getId(), new UpdateLeadContactRequest(
                "Nikhil Rao", null, "9800000010", "Pune", "VILLA", "B20_40L"));

        Lead saved = leads.findById(fromEmail.getId()).orElseThrow();
        assertEquals("9800000010", saved.getContactPhone());
        assertEquals("VILLA", saved.getPropertyType());
        // The whole point: it lands on the customer's own account too.
        User customer = saved.getCustomer();
        assertEquals("Nikhil Rao", customer.getName());
        assertEquals("9800000010", customer.getPhone());
        assertEquals("Pune", customer.getCity());
        // Blank fields leave what is already there alone.
        leadService.updateContact(director, fromEmail.getId(), new UpdateLeadContactRequest(
                "Nikhil Rao", null, null, null, null, null));
        assertEquals("9800000010", leads.findById(fromEmail.getId()).orElseThrow().getContactPhone());
    }

    @Test void theSignInAddressAndAnotherAccountsPhoneAreBothProtected() {
        LeadSummaryDto captured = capture("Riya Shah", "riya@home.test", "9800000001");
        contact(captured.id());
        assertThrows(BadRequestException.class, () -> leadService.updateContact(director, captured.id(),
                new UpdateLeadContactRequest("Riya Shah", "someone.else@home.test", null, null, null, null)));
        // director@conversion.test already holds this number
        director.setPhone("9800000099");
        users.save(director);
        assertThrows(BadRequestException.class, () -> leadService.updateContact(director, captured.id(),
                new UpdateLeadContactRequest("Riya Shah", null, "9800000099", null, null, null)));
    }

    @Test void aStaffAddressIsNeverTurnedIntoACustomerAndLostConvertsNobody() {
        LeadSummaryDto staffEmail = capture("Director", "director@conversion.test", "9800000003");
        contact(staffEmail.id());
        assertNull(leads.findById(staffEmail.id()).orElseThrow().getCustomer());

        LeadSummaryDto dead = capture("Gone", "gone@home.test", "9800000004");
        leadService.changeStage(director, dead.id(), new StageChangeRequest("LOST", "No budget"));
        assertNull(leads.findById(dead.id()).orElseThrow().getCustomer());
        assertFalse(users.findByEmail("gone@home.test").isPresent());
    }
}
