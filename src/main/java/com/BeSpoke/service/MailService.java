package com.BeSpoke.service;

import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Transactional email. When {@code app.mail.enabled} is false — the default for
 * development — the whole message is logged at INFO behind a {@code [MAIL]} prefix
 * instead of being sent, so generated passwords are readable in the console.
 * A send failure is always swallowed: no notification may fail a request.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    /** Null when spring.mail.host is unset — the auto-configuration skips the bean. */
    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean enabled;
    private final String from;
    private final String appUrl;

    public MailService(ObjectProvider<JavaMailSender> mailSender,
                       @Value("${app.mail.enabled:false}") boolean enabled,
                       @Value("${app.mail.from:BeSpoke <no-reply@bespoke.in>}") String from,
                       @Value("${app.mail.app-url:http://localhost:3000}") String appUrl) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
        this.appUrl = appUrl;
    }

    public void send(String to, String subject, String body) {
        log.error("PRINTING TO %s %s".formatted(to, enabled));
        log.error("[MAIL] disabled — would send to {}\n  from: {}\n  subject: {}\n  {}",
                to, from, subject, body.replace("\n", "\n  "));
        if (to == null || to.isBlank()) {
            return;
        }
        if (!enabled) {
            log.info("[MAIL] disabled — would send to {}\n  from: {}\n  subject: {}\n  {}",
                    to, from, subject, body.replace("\n", "\n  "));
            return;
        }
        try {
            JavaMailSender sender = mailSender.getIfAvailable();
            if (sender == null) {
                log.warn("[MAIL] app.mail.enabled=true but spring.mail.host is not configured;"
                        + " dropping mail to {} ({})", to, subject);
                return;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
            log.info("[MAIL] sent to {} ({})", to, subject);
        } catch (Exception ex) {
            log.warn("[MAIL] failed to send to {} ({}): {}", to, subject, ex.getMessage());
        }
    }

    public void customerSignedUp(User customer) {
        send(customer.getEmail(), "Welcome to BeSpoke",
                "Hi " + customer.getName() + ",\n\n"
                        + "Your BeSpoke account is ready. Sign in any time with a one-time code"
                        + " we email you:\n"
                        + appUrl + "/login\n\n"
                        + "Tell us about your home so we can match you with the right designer:\n"
                        + appUrl + "/welcome\n\n"
                        + "— BeSpoke");
    }

    public void loginOtp(User user, String code) {
        send(user.getEmail(), "Your BeSpoke login code",
                "Hi " + user.getName() + ",\n\n"
                        + "Your BeSpoke login code is: " + code + " . It expires in 10 minutes.\n\n"
                        + "— BeSpoke");
    }

    /** Partner "forgot password": the code that lets them set a new one. */
    public void passwordResetCode(User user, String code) {
        send(user.getEmail(), "Reset your BeSpoke password",
                "Hi " + user.getName() + ",\n\n"
                        + "Your password reset code is: " + code + " . It expires in 10 minutes.\n\n"
                        + "Enter it here to choose a new password:\n"
                        + appUrl + "/partner/login\n\n"
                        + "If you didn't ask for this, ignore this email — your password"
                        + " stays as it is.\n\n"
                        + "— BeSpoke");
    }

    public void passwordChanged(User user) {
        send(user.getEmail(), "Your BeSpoke password was changed",
                "Hi " + user.getName() + ",\n\n"
                        + "Your password has just been changed. If this wasn't you, reply to"
                        + " this email straight away.\n\n"
                        + "— BeSpoke");
    }

    /** Called when the customer submits their project brief (requirement form). */
    public void briefSubmitted(User customer) {
        send(customer.getEmail(), "Thanks — your project brief is in",
                "Hi " + customer.getName() + ",\n\n"
                        + "Your project brief has been submitted.\n"
                        + "Your designer will review the brief and reach out shortly.\n\n"
                        + "— BeSpoke");
    }

    public void staffAccountCreated(User staff, String password) {
        send(staff.getEmail(), "Your BeSpoke account",
                "Hi " + staff.getName() + ",\n\n"
                        + "An account has been created for you"
                        + (staff.getCompany() != null ? " at " + staff.getCompany().getName() : "")
                        + " as " + staff.getRole().name() + ".\n\n"
                        + "Email: " + staff.getEmail() + "\n"
                        + "Password: " + password + "\n\n"
                        + "Sign in: " + appUrl + "/login\n\n"
                        + "— BeSpoke");
    }

    public void companyOnboarded(Company company, User director, String password) {
        send(director.getEmail(), company.getName() + " is onboarded on BeSpoke",
                "Hi " + director.getName() + ",\n\n"
                        + company.getName() + " has been onboarded on BeSpoke and you are its director.\n\n"
                        + "Email: " + director.getEmail() + "\n"
                        + "Password: " + password + "\n\n"
                        + "Sign in: " + appUrl + "/login\n\n"
                        + "Complete your KYC to start receiving leads.\n\n"
                        + "— BeSpoke");
    }

    /** Sent to the receiving studio's director and sales manager when BeSpoke routes a lead. */
    public void leadRouted(User recipient, Lead lead) {
        send(recipient.getEmail(), "New lead from BeSpoke: " + lead.getContactName(),
                "Hi " + recipient.getName() + ",\n\n"
                        + "BeSpoke has routed a new project enquiry to you.\n\n"
                        + "Customer: " + lead.getContactName() + "\n"
                        + "City: " + lead.getCity() + "\n"
                        + "Property: " + (lead.getPropertyType() != null ? lead.getPropertyType() : "—") + "\n"
                        + "Budget: " + (lead.getBudgetBand() != null ? lead.getBudgetBand() : "—") + "\n\n"
                        + "Accept it in your dashboard: " + appUrl + "/studio/leads/" + lead.getId() + "\n\n"
                        + "— BeSpoke");
    }

    public void leadAccepted(User customer, String studioName) {
        send(customer.getEmail(), studioName + " has accepted your project",
                "Hi " + customer.getName() + ",\n\n"
                        + studioName + " has accepted your project and will be in touch shortly.\n"
                        + "You can now message them from your dashboard: " + appUrl + "/my\n\n"
                        + "— BeSpoke");
    }

    /** Called by DrawingService when a drawing is approved (V3 §5). */
    public void drawingApproved(User customer, String title) {
        send(customer.getEmail(), "A new design is ready to view",
                "Hi " + customer.getName() + ",\n\n"
                        + "\"" + title + "\" has been approved and is ready to view.\n"
                        + appUrl + "/my/designs\n\n"
                        + "— BeSpoke");
    }
}
