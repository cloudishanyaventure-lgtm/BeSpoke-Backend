package com.BeSpoke.service;

import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.PartnerApplication;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.User;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Customer names, studio names and mail subjects all land inside the HTML, so the escaping
 * is the part worth a test — a name with a tag in it must not be able to rewrite the email.
 * The same run drops rendered samples in build/mail-preview for eyeballing in a browser.
 */
class MailTemplatesTest {

    /** Captures the HTML instead of sending it. */
    private static class Captor extends MailService {
        final Map<String, String> sent = new LinkedHashMap<>();

        Captor() {
            super(null, null, false, "BeSpoke <noreply@bespokedesign.in>",
                    "https://crm.bespokedesign.in", "contact@bespokedesign.in");
        }

        @Override
        public void send(String to, String subject, String body, String html) {
            sent.put(subject, html);
        }
    }

    private static User user(String name) {
        return new User(name, "someone@example.com", "hash", Role.CUSTOMER);
    }

    private static PartnerApplication application() {
        PartnerApplication app = new PartnerApplication();
        app.setCompanyName("Atelier Kaya");
        app.setCity("Gurgaon");
        app.setContactName("Sara Fernandes");
        app.setContactEmail("sara@atelierkaya.in");
        app.setContactPhone("9810012345");
        return app;
    }

    private static Lead lead(String contactName) {
        Lead lead = new Lead();
        lead.setId(11L);
        lead.setContactName(contactName);
        lead.setContactEmail("poonam@example.com");
        lead.setCity("Gurgaon");
        lead.setPropertyType("APARTMENT");
        lead.setBudgetBand("L10_25");
        return lead;
    }

    @Test
    void escapesNamesSoAMailCannotBeRewritten() {
        Captor mail = new Captor();
        mail.customerSignedUp(user("<script>alert(1)</script>Eve"));
        String html = mail.sent.values().iterator().next();
        assertFalse(html.contains("<script>"), "a name must never reach the markup as a tag");
        assertTrue(html.contains("&lt;script&gt;"));
    }

    @Test
    void escapesEveryUserSuppliedSlot() {
        String hostile = "\"><b>x";
        // page() takes pre-built heading HTML, so the escaping lives in h() — both halves of it.
        assertFalse(MailTemplates.h(hostile).contains("<b>x"));
        assertFalse(MailTemplates.h(hostile, hostile).contains("<b>x"));
        assertTrue(MailTemplates.h("Welcome,", "Eve.").contains("<em style=\"font-style:italic;"));
        assertFalse(MailTemplates.facts("Label", hostile).contains("<b>x"));
        assertFalse(MailTemplates.code(hostile, "note").contains("<b>x"));
        assertFalse(MailTemplates.note(hostile).contains("<b>x"));
        assertFalse(MailTemplates.button("https://x", hostile).contains("<b>x"));
    }

    @Test
    void everyTemplateRendersAWholeDocument() throws Exception {
        Captor mail = new Captor();
        mail.customerSignedUp(user("Poonam Shukla"));
        mail.loginOtp(user("Poonam Shukla"), "482913");
        mail.passwordResetCode(user("Rohan Mehta"), "177569");
        mail.passwordChanged(user("Rohan Mehta"));
        mail.briefSubmitted(user("Poonam Shukla"));
        mail.staffAccountCreated(user("Nishant Rao"), "Kq7fMx2be9");
        mail.companyOnboarded(new Company("Atelier Kaya", "atelier-kaya"), user("Sara Fernandes"), "Kq7fMx2be9");
        mail.partnerApplicationReceived(application());
        mail.partnerApproved(new Company("Atelier Kaya", "atelier-kaya"), user("Sara Fernandes"), "Kq7fMx2be9");
        mail.leadReceived(lead("Poonam Shukla"), "i need design");
        mail.leadRouted(user("Sara Fernandes"), lead("Poonam Shukla"));
        mail.leadAccepted(user("Poonam Shukla"), "Atelier Kaya");
        mail.quoteSent(lead("Poonam Shukla"), "Full home — 3BHK", 2);
        mail.invoiceSent(user("Poonam Shukla"), "INV-2026-014", "1,45,000", "12 Sep 2026");
        mail.orderPlaced(user("Poonam Shukla"), "BeSpoke Living", "24,600");
        mail.kycDecision(user("Sara Fernandes"), new Company("Atelier Kaya", "atelier-kaya"), true);
        mail.drawingApproved(user("Poonam Shukla"), "Living room — final");

        Path dir = Path.of("build", "mail-preview");
        Files.createDirectories(dir);
        int i = 0;
        for (Map.Entry<String, String> entry : mail.sent.entrySet()) {
            String html = entry.getValue();
            assertTrue(html.startsWith("<!DOCTYPE html"), entry.getKey() + " is not a document");
            assertTrue(html.contains("</html>"), entry.getKey() + " is truncated");
            assertFalse(html.contains("%s") || html.contains("%1$s"),
                    entry.getKey() + " has an unfilled placeholder");
            Files.writeString(dir.resolve("%02d-%s.html".formatted(
                    ++i, entry.getKey().replaceAll("[^A-Za-z0-9]+", "-"))), html);
        }
        assertTrue(i >= 15, "expected every template to render, got " + i);
    }
}
