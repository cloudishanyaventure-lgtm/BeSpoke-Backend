package com.BeSpoke.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Body extraction is the one part of the contact@ poller that can silently file empty leads. */
class InboxServiceTest {

    private static MimeMessage message() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    @Test
    void treatsNoReplySendersAsMachines() {
        // The live incident: Google's security alert became a lead and got a reply.
        assertTrue(InboxService.isMachineAddress("no-reply@accounts.google.com"));
        assertTrue(InboxService.isMachineAddress("noreply@somevendor.com"));
        assertTrue(InboxService.isMachineAddress("donotreply@bank.example"));
        assertTrue(InboxService.isMachineAddress("mailer-daemon@googlemail.com"));
        assertTrue(InboxService.isMachineAddress("noreply@bespokedesign.in"));
        assertTrue(InboxService.isMachineAddress(""));
        // A real person must still get through — including one whose name contains "reply".
        assertFalse(InboxService.isMachineAddress("prasantshukla89@gmail.com"));
        assertFalse(InboxService.isMachineAddress("replykumar@gmail.com"));
    }

    @Test
    void readsPlainTextBody() throws Exception {
        MimeMessage mail = message();
        mail.setText("Need a 3BHK done in Gurgaon.");
        mail.saveChanges();
        assertEquals("Need a 3BHK done in Gurgaon.", InboxService.textOf(mail));
    }

    @Test
    void prefersPlainTextPartOverHtmlInAlternative() throws Exception {
        MimeBodyPart html = new MimeBodyPart();
        html.setContent("<p>Need a <b>3BHK</b> done.</p>", "text/html");
        MimeBodyPart plain = new MimeBodyPart();
        plain.setText("Need a 3BHK done.");
        MimeMultipart body = new MimeMultipart("alternative");
        body.addBodyPart(html);   // html first on purpose: order must not decide
        body.addBodyPart(plain);

        MimeMessage mail = message();
        mail.setContent(body);
        mail.saveChanges();
        assertEquals("Need a 3BHK done.", InboxService.textOf(mail));
    }

    @Test
    void fallsBackToStrippedHtmlWhenThereIsNoPlainPart() throws Exception {
        MimeBodyPart html = new MimeBodyPart();
        html.setContent("<style>p{color:red}</style><p>Budget is &lt;15L</p>", "text/html");
        MimeMultipart body = new MimeMultipart("alternative");
        body.addBodyPart(html);

        MimeMessage mail = message();
        mail.setContent(body);
        mail.saveChanges();
        assertEquals("Budget is <15L", InboxService.textOf(mail));
    }

    @Test
    void survivesAnAttachmentOnlyMail() throws Exception {
        MimeBodyPart pdf = new MimeBodyPart();
        pdf.setContent(new byte[]{1, 2, 3}, "application/pdf");
        MimeMultipart body = new MimeMultipart();
        body.addBodyPart(pdf);

        MimeMessage mail = message();
        mail.setContent(body);
        mail.saveChanges();
        assertTrue(InboxService.textOf(mail).isBlank());
    }
}
