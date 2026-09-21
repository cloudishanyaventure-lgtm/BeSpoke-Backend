package com.BeSpoke;

import com.BeSpoke.config.SeedRunner;
import com.BeSpoke.entity.*;
import com.BeSpoke.repository.*;
import com.BeSpoke.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real HTTP security, transactions, relational history and notifications; isolated from local data. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:designworkflow;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.mail.enabled=false", "app.inbox.enabled=false", "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
@Transactional
class DesignWorkflowIntegrationTest {
    @MockBean SeedRunner seedRunner;
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired CompanyRepository companies;
    @Autowired LeadRepository leads;
    @Autowired DrawingRepository drawings;
    @Autowired AppNotificationRepository notifications;
    @Autowired LeadActivityRepository activities;
    @Autowired AuditEventRepository audits;
    @Autowired EntityManager em;
    @Autowired JwtService jwt;
    @Autowired ObjectMapper json;
    User customer, stranger, designer, manager, outsider, sales;
    Lead lead;

    @BeforeEach void setup() {
        Company studio = companies.save(new Company("Review studio", "review-studio"));
        customer = user("Customer", Role.CUSTOMER, null);
        stranger = user("Stranger", Role.CUSTOMER, null);
        designer = user("Designer", Role.DESIGNER, studio);
        manager = user("Manager", Role.DESIGN_MANAGER, studio);
        sales = user("Sales", Role.SALES_EXECUTIVE, studio);
        outsider = user("Outsider", Role.DIRECTOR, companies.save(new Company("Other studio", "review-other")));
        lead = lead(customer, studio);
    }
    User user(String name, Role role, Company company) {
        User user = new User(name, name.toLowerCase() + "@review.test", "unused", role);
        user.setCompany(company);
        return users.save(user);
    }
    Lead lead(User owner, Company company) {
        Lead value = new Lead();
        value.setCustomer(owner); value.setCompany(company); value.setAssignedDesigner(designer);
        value.setContactName("Customer"); value.setContactEmail("customer@review.test");
        value.setContactPhone("1234567890"); value.setCity("Pune");
        value.setSource(LeadSource.WEBSITE); value.setStatus(LeadStatus.CONTACTED);
        return leads.saveAndFlush(value);
    }
    String token(User user) { return "Bearer " + jwt.generateToken(user); }
    String privateImage() throws Exception {
        var out=new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",out);
        String body=mvc.perform(multipart("/api/project-documents")
                .file(new org.springframework.mock.web.MockMultipartFile("file","design.png","image/png",out.toByteArray()))
                .param("leadId",lead.getId().toString()).param("type","DESIGN").header("Authorization",token(designer)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("fileUrl").asText();
    }
    String payload(Long previous) throws Exception {
        var data = new java.util.LinkedHashMap<String, Object>(Map.of("title", "Kitchen", "floorLabel", "Ground floor",
                "spaceLabel", "Kitchen", "fileUrl", privateImage()));
        if (previous != null) data.put("previousRevisionId", previous);
        return json.writeValueAsString(data);
    }
    long create(Long previous) throws Exception {
        String result = mvc.perform(post("/api/leads/" + lead.getId() + "/drawings")
                .header("Authorization", token(designer)).contentType(MediaType.APPLICATION_JSON).content(payload(previous)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("WIP"))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(result).get("id").asLong();
    }
    void share(long id) throws Exception {
        mvc.perform(post("/api/drawings/" + id + "/submit").header("Authorization", token(designer)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
        mvc.perform(post("/api/drawings/" + id + "/approve").header("Authorization", token(manager)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test void revisionApprovalPreservesFilesFeedbackActorsAndNotifiesBothSides() throws Exception {
        long v1 = create(null);
        mvc.perform(get("/api/my/drawings").header("Authorization", token(customer)))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        share(v1);
        assertEquals(1, notifications.countByRecipientAndReadAtIsNull(customer));
        assertEquals(1, notifications.countByRecipientAndReadAtIsNull(manager));
        mvc.perform(get("/api/my/drawings").header("Authorization", token(customer)))
                .andExpect(jsonPath("$[0].revisionNumber").value(1))
                .andExpect(jsonPath("$[0].approvedByName").doesNotExist())
                .andExpect(jsonPath("$[0].pendingWith").doesNotExist());
        mvc.perform(post("/api/my/drawings/" + v1 + "/request-changes").header("Authorization", token(customer))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Use lighter oak\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CHANGES_REQUESTED"))
                .andExpect(jsonPath("$.customerDecidedById").value(customer.getId()));
        assertEquals(1, notifications.countByRecipientAndReadAtIsNull(designer));
        long v2 = create(v1);
        Drawing old = drawings.findById(v1).orElseThrow();
        assertEquals("Use lighter oak", old.getRejectionReason());
        assertEquals(DrawingStatus.CHANGES_REQUESTED, old.getStatus());
        assertTrue(old.getFileUrl().startsWith("/api/documents/"));
        assertNotNull(old.getDocument());
        assertNotNull(old.getSupersededAt());
        mvc.perform(post("/api/my/drawings/" + v1 + "/approve").header("Authorization", token(customer)))
                .andExpect(status().isConflict());
        share(v2);
        mvc.perform(post("/api/my/drawings/" + v2 + "/approve").header("Authorization", token(customer)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FINAL"))
                .andExpect(jsonPath("$.revisionNumber").value(2)).andExpect(jsonPath("$.previousRevisionId").value(v1))
                .andExpect(jsonPath("$.customerApprovedAt").isNotEmpty());
        assertEquals(2, notifications.countByRecipientAndReadAtIsNull(designer));
        em.flush(); em.clear();
        Drawing approved = drawings.findById(v2).orElseThrow();
        assertEquals(customer.getId(), approved.getCustomerDecidedBy().getId());
        assertNotNull(approved.getCustomerDecidedAt());
        assertEquals(2, drawings.count());
        assertTrue(audits.findAll().stream().anyMatch(a -> a.getAction().equals("DRAWING_CUSTOMER_APPROVED")));
        assertTrue(activities.findByLeadOrderByCreatedAtAsc(lead).stream().anyMatch(a -> a.getBody().contains("V2")));
        // A later design change creates V3 without erasing the signed-off V2.
        long v3 = create(v2);
        assertEquals(3, drawings.findById(v3).orElseThrow().getRevisionNumber());
        assertEquals(DrawingStatus.FINAL, drawings.findById(v2).orElseThrow().getStatus());
        assertNotNull(drawings.findById(v2).orElseThrow().getCustomerApprovedAt());
    }

    @Test void ownershipInternalDraftsAndStaffSignOffAreEnforcedServerSide() throws Exception {
        long id = create(null);
        mvc.perform(post("/api/my/drawings/" + id + "/approve").header("Authorization", token(customer))).andExpect(status().isNotFound());
        mvc.perform(get("/api/leads/" + lead.getId() + "/drawings").header("Authorization", token(stranger))).andExpect(status().isForbidden());
        mvc.perform(post("/api/leads/" + lead.getId() + "/drawings").header("Authorization", token(sales))
                .contentType(MediaType.APPLICATION_JSON).content(payload(null))).andExpect(status().isForbidden());
        share(id);
        for (User hidden : List.of(stranger, outsider)) {
            mvc.perform(post("/api/drawings/" + id + "/approve").header("Authorization", token(hidden)))
                    .andExpect(status().is(hidden == stranger ? 403 : 404));
        }
        mvc.perform(post("/api/my/drawings/" + id + "/approve").header("Authorization", token(stranger))).andExpect(status().isNotFound());
        mvc.perform(post("/api/drawings/" + id + "/finalize").header("Authorization", token(manager))).andExpect(status().isForbidden());
        mvc.perform(get("/api/my/drawings")).andExpect(status().isUnauthorized());
    }

    @Test void retryIsIdempotentAndConflictingDecisionIsRejected() throws Exception {
        long id = create(null); share(id);
        for (int i = 0; i < 2; i++) mvc.perform(post("/api/my/drawings/" + id + "/approve")
                .header("Authorization", token(customer))).andExpect(status().isOk());
        assertEquals(1, notifications.countByRecipientAndReadAtIsNull(designer));
        assertEquals(1, audits.findAll().stream().filter(a -> a.getAction().equals("DRAWING_CUSTOMER_APPROVED")).count());
        mvc.perform(post("/api/my/drawings/" + id + "/request-changes").header("Authorization", token(customer))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Conflicting decision\"}")).andExpect(status().isConflict());
    }

    @Test void revisionsCannotBranchOrReferenceAnotherLead() throws Exception {
        long id = create(null); share(id);
        mvc.perform(post("/api/leads/" + lead.getId() + "/drawings").header("Authorization", token(designer))
                .contentType(MediaType.APPLICATION_JSON).content(payload(id))).andExpect(status().isConflict());
        mvc.perform(post("/api/my/drawings/" + id + "/request-changes").header("Authorization", token(customer))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"New layout\"}")).andExpect(status().isOk());
        Lead another = lead(customer, designer.getCompany());
        mvc.perform(post("/api/leads/" + another.getId() + "/drawings").header("Authorization", token(designer))
                .contentType(MediaType.APPLICATION_JSON).content(payload(id))).andExpect(status().isNotFound());
        create(id);
        mvc.perform(post("/api/leads/" + lead.getId() + "/drawings").header("Authorization", token(designer))
                .contentType(MediaType.APPLICATION_JSON).content(payload(id))).andExpect(status().isConflict());
    }

    @Test void customerCanReviewDesignsOnOlderOwnedLeads() throws Exception {
        long id = create(null); share(id);
        lead(customer, designer.getCompany());
        mvc.perform(get("/api/my/drawings").header("Authorization", token(customer)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(post("/api/my/drawings/" + id + "/approve").header("Authorization", token(customer)))
                .andExpect(status().isOk());
    }

    @Test void staleWritesCannotOverwriteADecision() throws Exception {
        long id = create(null);
        Drawing stale = drawings.findById(id).orElseThrow();
        em.detach(stale);
        Drawing current = drawings.findById(id).orElseThrow();
        current.setStatus(DrawingStatus.FINAL);
        em.flush();
        stale.setStatus(DrawingStatus.WIP);
        assertThrows(jakarta.persistence.OptimisticLockException.class, () -> em.merge(stale));
    }

    @Test void blankFeedbackAndResubmittingRejectedFilesAreRejected() throws Exception {
        long id = create(null);
        mvc.perform(post("/api/drawings/" + id + "/submit").header("Authorization", token(designer))).andExpect(status().isOk());
        mvc.perform(post("/api/drawings/" + id + "/reject").header("Authorization", token(manager))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Fix dimensions\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/drawings/" + id + "/submit").header("Authorization", token(designer))).andExpect(status().isConflict());
        long next = create(id); share(next);
        mvc.perform(post("/api/my/drawings/" + next + "/request-changes").header("Authorization", token(customer))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"   \"}")).andExpect(status().isBadRequest());
    }

    @Test void walkInFinalizationIsAttributedWithoutFabricatingCustomerApproval() throws Exception {
        lead.setCustomer(null); leads.saveAndFlush(lead);
        long id = create(null); share(id);
        mvc.perform(post("/api/drawings/" + id + "/finalize").header("Authorization", token(manager)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FINAL"))
                .andExpect(jsonPath("$.finalizedByName").value("Manager"))
                .andExpect(jsonPath("$.finalizedAt").isNotEmpty())
                .andExpect(jsonPath("$.customerApprovedAt").doesNotExist());
    }

    /** Drawings are not always images: a PDF sheet uploads, becomes a drawing and downloads as a PDF. */
    @Test void pdfAndOtherFileTypesUploadAsDesigns() throws Exception {
        String pdf = mvc.perform(multipart("/api/project-documents")
                .file(new org.springframework.mock.web.MockMultipartFile("file", "plan.pdf", "application/pdf",
                        "%PDF-1.7\n1 0 obj\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII)))
                .param("leadId", lead.getId().toString()).param("type", "DESIGN").header("Authorization", token(designer)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.contentType").value("application/pdf"))
                .andReturn().getResponse().getContentAsString();
        String fileUrl = json.readTree(pdf).get("fileUrl").asText();
        mvc.perform(post("/api/leads/" + lead.getId() + "/drawings").header("Authorization", token(designer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("title", "Kitchen", "floorLabel", "Ground floor",
                        "spaceLabel", "Kitchen", "fileUrl", fileUrl))))
                .andExpect(status().isCreated());
        mvc.perform(get(fileUrl).header("Authorization", token(designer)))
                .andExpect(status().isOk()).andExpect(header().string("Content-Type", "application/pdf"));

        // Anything else the browser can't sniff is still stored, as an opaque download.
        mvc.perform(multipart("/api/project-documents")
                .file(new org.springframework.mock.web.MockMultipartFile("file", "plan.dwg", "application/acad",
                        new byte[]{'A', 'C', '1', '0', '2', '7'}))
                .param("leadId", lead.getId().toString()).param("type", "DESIGN").header("Authorization", token(designer)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.contentType").value("application/octet-stream"));
    }
}
