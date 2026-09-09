package com.BeSpoke;

import com.BeSpoke.config.SeedRunner;
import com.BeSpoke.entity.*;
import com.BeSpoke.repository.*;
import com.BeSpoke.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real security filters, service wiring, persistence and serialization; never touches the developer database. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:workhub;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.mail.enabled=false", "app.inbox.enabled=false", "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
@Transactional
class WorkHubIntegrationTest {
    @MockBean SeedRunner seedRunner;
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired CompanyRepository companies;
    @Autowired StaffTaskRepository tasks;
    @Autowired TaskCommentRepository comments;
    @Autowired AppNotificationRepository notifications;
    @Autowired ProductRepository products;
    @Autowired ShopOrderRepository orders;
    @Autowired JwtService jwt;
    @Autowired ObjectMapper json;
    User director, manager, designer, peer, otherManager, customer, outsider;
    Company studio;
    @BeforeEach void setup() {
        studio = companies.save(new Company("Test studio", "work-test"));
        director = user("Director", Role.DIRECTOR, studio, null);
        manager = user("Manager", Role.DESIGN_MANAGER, studio, director);
        otherManager = user("Other manager", Role.DESIGN_MANAGER, studio, director);
        designer = user("Designer", Role.DESIGNER, studio, manager);
        peer = user("Peer", Role.DESIGNER, studio, otherManager);
        customer = user("Customer", Role.CUSTOMER, null, null);
        outsider = user("Outsider", Role.DIRECTOR, companies.save(new Company("Other", "work-other")), null);
    }
    User user(String name, Role role, Company company, User parent) {
        User u = new User(name, name.replace(" ", "") + "@work.test", "unused", role);
        u.setCompany(company); u.setReportsTo(parent); return users.save(u);
    }
    String token(User u) { return "Bearer " + jwt.generateToken(u); }
    Long create(String visibility) throws Exception {
        String response = mvc.perform(post("/api/tasks").header("Authorization", token(manager)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("title", "Oak samples", "assigneeId", designer.getId(), "visibility", visibility, "priority", "HIGH"))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.canUpdate").value(true))
                .andExpect(jsonPath("$.visibility").value(visibility)).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }
    @Test void catalogueDimensionsPersistValidateAndRespectVendorOwnership() throws Exception {
        Company vendor = companies.save(new Company("Spatial vendor", "spatial-vendor"));
        vendor.setType(CompanyType.VENDOR); vendor.setKycStatus(KycStatus.VERIFIED); companies.save(vendor);
        User supplier = user("Spatial supplier", Role.DIRECTOR, vendor, null);
        String payload = "{\"name\":\"Oak table\",\"category\":\"FURNITURE\",\"price\":2500,\"spatial\":{\"widthMm\":1200,\"depthMm\":600,\"heightMm\":450,\"finishColor\":\"#b89572\",\"modelUrl\":\"https://example.com/table.glb\"}}";
        String created = mvc.perform(post("/api/vendor/products").header("Authorization", token(supplier)).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.spatial.widthMm").value(1200)).andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();
        mvc.perform(get("/api/shop/products/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.spatial.heightMm").value(450));
        for (String bad : List.of(payload.replace("1200", "0"), payload.replace("1200", "50001"), payload.replace("https://example.com/table.glb", "javascript:alert(1)"), payload.replace("#b89572", "red"))) {
            mvc.perform(put("/api/vendor/products/" + id).header("Authorization", token(supplier)).contentType(MediaType.APPLICATION_JSON).content(bad)).andExpect(status().isBadRequest());
        }
        Company rival = companies.save(new Company("Spatial rival", "spatial-rival")); rival.setType(CompanyType.VENDOR); companies.save(rival);
        User rivalUser = user("Spatial rival owner", Role.DIRECTOR, rival, null);
        mvc.perform(put("/api/vendor/products/" + id).header("Authorization", token(rivalUser)).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isNotFound());
        mvc.perform(put("/api/vendor/products/" + id).header("Authorization", token(supplier)).contentType(MediaType.APPLICATION_JSON).content("{\"price\":2600}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.spatial.widthMm").value(1200));
        mvc.perform(put("/api/vendor/products/" + id).header("Authorization", token(supplier)).contentType(MediaType.APPLICATION_JSON).content(payload.replace("https://example.com/table.glb", "")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.spatial.modelUrl").value(""));
    }

    @Test void privateWorkNeverLeaksToManagersOtherTenantsOrCustomers() throws Exception {
        Long id = create("PRIVATE");
        for (User hidden : List.of(director, peer, otherManager, outsider)) {
            mvc.perform(get("/api/tasks").header("Authorization", token(hidden))).andExpect(status().isOk()).andExpect(content().json("[]"));
            mvc.perform(get("/api/tasks/" + id).header("Authorization", token(hidden))).andExpect(status().isNotFound());
            mvc.perform(get("/api/tasks/" + id + "/comments").header("Authorization", token(hidden))).andExpect(status().isNotFound());
            mvc.perform(put("/api/tasks/" + id + "/status?status=DONE").header("Authorization", token(hidden))).andExpect(status().isNotFound());
        }
        mvc.perform(get("/api/tasks").header("Authorization", token(customer))).andExpect(status().isForbidden());
        mvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/tasks").header("Authorization", token(designer))).andExpect(jsonPath("$[0].id").value(id));
    }
    @Test void reportingLinesPreventLateralUpwardAndCrossBranchAssignment() throws Exception {
        for (User target : List.of(director, otherManager, peer)) {
            mvc.perform(post("/api/tasks").header("Authorization", token(manager)).contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("title", "Invalid work", "assigneeId", target.getId())))).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/tasks/people").header("Authorization", token(manager))).andExpect(status().isOk());
        designer.setActive(false); users.saveAndFlush(designer);
        mvc.perform(post("/api/tasks").header("Authorization", token(manager)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("title", "Inactive work", "assigneeId", designer.getId())))).andExpect(status().isBadRequest());
    }
    @Test void publicDiscussionMentionsNotifyAndAssigneeCanFinishWork() throws Exception {
        Long id = create("PUBLIC");
        mvc.perform(get("/api/tasks").header("Authorization", token(peer))).andExpect(jsonPath("$[0].canUpdate").value(false));
        mvc.perform(post("/api/tasks/" + id + "/comments").header("Authorization", token(peer)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("body", "Samples are ready", "mentionIds", List.of(designer.getId()))))).andExpect(status().isCreated());
        assertTrue(comments.findTop200ByTaskIdOrderByIdDesc(id).get(0).getBody().startsWith("gcm:"));
        mvc.perform(get("/api/tasks/" + id + "/comments").header("Authorization", token(designer))).andExpect(jsonPath("$[0].body").value("Samples are ready"));
        for (String state : List.of("IN_PROGRESS", "BLOCKED", "DONE", "OPEN")) {
            mvc.perform(put("/api/tasks/" + id + "/status?status=" + state).header("Authorization", token(designer))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value(state));
            tasks.flush();
        }
        assertNull(tasks.findById(id).orElseThrow().getCompletedAt());
        assertEquals(5, comments.findTop200ByTaskIdOrderByIdDesc(id).size());
        assertTrue(notifications.countByRecipientAndReadAtIsNull(designer) > 0);
        mvc.perform(put("/api/tasks/" + id + "/status?status=DONE").header("Authorization", token(peer))).andExpect(status().isForbidden());
        mvc.perform(get("/api/tasks/" + id).header("Authorization", token(outsider))).andExpect(status().isNotFound());
    }
    @Test void privateMentionsCannotExpandAccessAndLegacyTasksStayPrivate() throws Exception {
        StaffTask legacy = tasks.saveAndFlush(new StaffTask(studio, "Legacy confidential work", designer, manager));
        mvc.perform(get("/api/tasks").header("Authorization", token(director))).andExpect(content().json("[]"));
        mvc.perform(post("/api/tasks/" + legacy.getId() + "/comments").header("Authorization", token(designer)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("body", "Cannot share", "mentionIds", List.of(director.getId()))))).andExpect(status().isBadRequest());
        assertEquals(0, comments.count());
    }
    @Test void existingTokensLosePrivilegesWhenRoleOrCompanyChanges() throws Exception {
        String oldToken = token(director);
        director.setRole(Role.DESIGNER); users.saveAndFlush(director);
        mvc.perform(get("/api/invoices").header("Authorization", oldToken)).andExpect(status().isForbidden());
        studio.setActive(false); companies.saveAndFlush(studio);
        mvc.perform(get("/api/tasks").header("Authorization", oldToken)).andExpect(status().isUnauthorized());
    }
    @Test void privateChatIsTenantScopedAndDisabledRecipientsCannotReceive() throws Exception {
        mvc.perform(post("/api/team-chat/" + designer.getId()).header("Authorization", token(manager)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Can you check the samples?\"}")).andExpect(status().isCreated());
        mvc.perform(get("/api/team-chat/" + manager.getId()).header("Authorization", token(designer)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].body").value("Can you check the samples?"));
        mvc.perform(get("/api/team-chat/" + manager.getId()).header("Authorization", token(outsider))).andExpect(status().isNotFound());
        designer.setActive(false); users.saveAndFlush(designer);
        mvc.perform(post("/api/team-chat/" + designer.getId()).header("Authorization", token(manager)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Cannot send\"}")).andExpect(status().isNotFound());
    }
    @Test void vendorCatalogueCustomerCheckoutAndDeliveryWorkTogether() throws Exception {
        Company vendor = new Company("Oak works", "oak-works");
        vendor.setType(CompanyType.VENDOR); vendor.setKycStatus(KycStatus.VERIFIED); vendor = companies.save(vendor);
        User supplier = user("Supplier", Role.DIRECTOR, vendor, null);
        String created = mvc.perform(post("/api/vendor/products").header("Authorization", token(supplier)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Oak chair\",\"category\":\"FURNITURE\",\"price\":2500}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long productId = json.readTree(created).get("id").asLong();
        mvc.perform(get("/api/shop/products/" + productId)).andExpect(status().isOk()).andExpect(jsonPath("$.vendor.name").value("Oak works"));
        String body = json.writeValueAsString(Map.of("items", List.of(Map.of("productId", productId, "qty", 2)), "shippingAddress", "Test address, Delhi", "phone", "9999999999", "checkoutKey", "checkout-0123456789", "expectedTotal", 5000));
        String response = mvc.perform(post("/api/orders").header("Authorization", token(customer)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$[0].total").value(5000)).andReturn().getResponse().getContentAsString();
        long id = json.readTree(response).get(0).get("id").asLong();
        mvc.perform(post("/api/orders").header("Authorization", token(customer)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$[0].id").value(id));
        assertEquals(1, orders.count());
        mvc.perform(post("/api/orders").header("Authorization", token(customer)).contentType(MediaType.APPLICATION_JSON).content(body.replace("Test address", "Changed address")))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/orders").header("Authorization", token(customer)).contentType(MediaType.APPLICATION_JSON).content(body.replace("checkout-0123456789", "checkout-9876543210").replace("5000", "4000")))
                .andExpect(status().isConflict());
        for (String state : List.of("CONFIRMED", "SHIPPED", "DELIVERED")) mvc.perform(put("/api/vendor/orders/" + id + "/status").header("Authorization", token(supplier)).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + state + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(state));
        mvc.perform(get("/api/my/orders").header("Authorization", token(customer))).andExpect(jsonPath("$[0].status").value("DELIVERED"));
        mvc.perform(put("/api/vendor/orders/" + id + "/status").header("Authorization", token(supplier)).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isBadRequest());
    }
    @Test void hiddenProductsAndOtherVendorsCannotBeChangedOrPurchased() throws Exception {
        Company vendor = new Company("Hidden works", "hidden-works"); vendor.setType(CompanyType.VENDOR); vendor.setKycStatus(KycStatus.VERIFIED); vendor = companies.save(vendor);
        User supplier = user("Supplier", Role.DIRECTOR, vendor, null);
        String created = mvc.perform(post("/api/vendor/products").header("Authorization", token(supplier)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Oak chair\",\"category\":\"FURNITURE\",\"price\":2500}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();
        Company rival = new Company("Rival supplier", "rival-supplier"); rival.setType(CompanyType.VENDOR); rival = companies.save(rival);
        User rivalUser = user("Rival user", Role.DIRECTOR, rival, null);
        mvc.perform(put("/api/vendor/products/" + id).header("Authorization", token(rivalUser)).contentType(MediaType.APPLICATION_JSON).content("{\"price\":1}"))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/vendor/products/" + id).header("Authorization", token(supplier)).contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/shop/products/" + id)).andExpect(status().isNotFound());
        mvc.perform(post("/api/orders").header("Authorization", token(customer)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("items", List.of(Map.of("productId", id, "qty", 1)), "shippingAddress", "Test address", "phone", "9999999999"))))
                .andExpect(status().isNotFound());
        assertEquals(0, orders.count());
    }
}
