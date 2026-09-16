package com.BeSpoke.service;

import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.CompanyType;
import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.PartnerApplication;
import com.BeSpoke.entity.StaffTask;
import com.BeSpoke.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import static com.BeSpoke.service.MailTemplates.button;
import static com.BeSpoke.service.MailTemplates.code;
import static com.BeSpoke.service.MailTemplates.esc;
import static com.BeSpoke.service.MailTemplates.facts;
import static com.BeSpoke.service.MailTemplates.h;
import static com.BeSpoke.service.MailTemplates.note;
import static com.BeSpoke.service.MailTemplates.p;
import static com.BeSpoke.service.MailTemplates.page;

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
    /** Lazy: MailService is constructed by nearly every service; the labels are read on demand. */
    private final ObjectProvider<PlatformOptionService> options;
    private final NotificationService notifications;
    private final boolean enabled;
    private final String from;
    private final String appUrl;
    /** Where BeSpoke's own notifications land — new signups, anything needing a human. */
    private final String internalTo;

    @org.springframework.beans.factory.annotation.Autowired
    public MailService(ObjectProvider<JavaMailSender> mailSender,
                       ObjectProvider<PlatformOptionService> options,
                       NotificationService notifications,
                       @Value("${app.mail.enabled:false}") boolean enabled,
                       @Value("${app.mail.from:BeSpoke <no-reply@bespoke.in>}") String from,
                       @Value("${app.mail.app-url:http://localhost:3000}") String appUrl,
                       @Value("${app.mail.internal:contact@bespokedesign.in}") String internalTo) {
        this.mailSender = mailSender;
        this.options = options;
        this.notifications = notifications;
        this.enabled = enabled;
        this.from = from;
        this.appUrl = appUrl;
        this.internalTo = internalTo;
    }

    /** Compatibility constructor for template-only tests and lightweight callers. */
    public MailService(ObjectProvider<JavaMailSender> mailSender,
                       ObjectProvider<PlatformOptionService> options,
                       boolean enabled,
                       String from,
                       String appUrl,
                       String internalTo) {
        this(mailSender, options, null, enabled, from, appUrl, internalTo);
    }

    /** Picklist label, or the raw code humanised when the option service isn't around (tests). */
    private String label(String listKey, String value) {
        PlatformOptionService service = options == null ? null : options.getIfAvailable();
        if (service != null) {
            return service.label(listKey, value);
        }
        return value == null || value.isBlank() ? "—" : PlatformOptionService.humanize(value);
    }

    private void notifyInApp(User recipient, String title, String body, String link) {
        if (notifications != null) {
            notifications.publish(recipient, title, body, link);
        }
    }

    /** Plain-text only — kept for anything without a designed counterpart. */
    public void send(String to, String subject, String body) {
        send(to, subject, body, null);
    }

    /**
     * Sends multipart/alternative when {@code html} is given: every client that can render
     * it does, and text-only readers, screen readers and spam filters still get the plain
     * part. The plain text is the source of truth for what the mail says.
     */
    public void send(String to, String subject, String body, String html) {
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
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, html != null, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            if (html != null) {
                helper.setText(body, html);
            } else {
                helper.setText(body);
            }
            sender.send(message);
            log.info("[MAIL] sent to {} ({})", to, subject);
        } catch (Exception ex) {
            // ERROR, with the cause: a dropped sign-in code locks a customer out of the
            // product, which is not a warning-level event however well we recover from it.
            log.error("[MAIL] failed to send to {} ({}) — {}", to, subject, ex.toString(), ex);
        }
    }

    /**
     * Says out loud, once, whether mail can actually leave this box. Every sign-in code,
     * generated password and reset code rides on this; a misconfiguration used to show up
     * only as customers who "never got the email".
     */
    @EventListener(ApplicationReadyEvent.class)
    public void reportMailState() {
        if (!enabled) {
            log.warn("[MAIL] app.mail.enabled=false — sign-in codes, passwords and reset codes"
                    + " will be LOGGED, not sent. Set MAIL_ENABLED=true and run with"
                    + " SPRING_PROFILES_ACTIVE=prod to actually deliver mail.");
        } else if (mailSender.getIfAvailable() == null) {
            log.error("[MAIL] app.mail.enabled=true but no mail sender exists (spring.mail.host"
                    + " unset — is SPRING_PROFILES_ACTIVE=prod?). Every email will be dropped.");
        } else {
            log.info("[MAIL] enabled — sending as {}", from);
        }
    }

    public void customerSignedUp(User customer) {
        String name = customer.getName();
        send(customer.getEmail(),
                "Welcome " + name + " to BeSpoke — let's find your designer",
                "Hi " + name + ",\n\n"
                        + "Your BeSpoke account is ready. Sign in any time with a one-time code"
                        + " we email you:\n"
                        + appUrl + "/login\n\n"
                        + "Tell us about your home so we can match you with the right designer:\n"
                        + appUrl + "/welcome\n\n"
                        + "— BeSpoke",
                page("Your account is ready — tell us about your home.",
                        "Welcome to BeSpoke",
                        h("Let's find your designer,", name + "."),
                        p("Your BeSpoke account is ready. There's no password to remember — we email"
                                + " you a one-time code whenever you sign in.")
                                + p("The next step is the part we love: tell us about your home — rooms,"
                                        + " style, budget, timeline — and we'll match you with a designer"
                                        + " who fits.")
                                + button(appUrl + "/welcome", "Tell us about your home")
                                + note("Already done? Your project lives at " + appUrl + "/my")));
    }

    public void loginOtp(User user, String code) {
        send(user.getEmail(), "Your BeSpoke login code",
                "Hi " + user.getName() + ",\n\n"
                        + "Your BeSpoke login code is: " + code + " . It expires in 10 minutes.\n\n"
                        + "— BeSpoke",
                page("Your one-time sign-in code, valid for 10 minutes.",
                        "Sign in",
                        h("Here's your code,", user.getName() + "."),
                        p("Enter this in the sign-in screen to open your project.")
                                + code(code, "Expires in 10 minutes")
                                + note("Didn't try to sign in? You can ignore this email — nobody can"
                                        + " get in without the code.")));
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
                        + "— BeSpoke",
                page("Your password reset code, valid for 10 minutes.",
                        "Password reset",
                        h("Let's get you back in,", user.getName() + "."),
                        p("Use this code to choose a new password.")
                                + code(code, "Expires in 10 minutes")
                                + button(appUrl + "/partner/login", "Set a new password")
                                + note("If you didn't ask for this, ignore this email — your password"
                                        + " stays exactly as it is.")));
    }

    /** DPDP account deletion: the code that confirms the request really came from the owner. */
    public void accountDeletionCode(User user, String code) {
        send(user.getEmail(), "Confirm deletion of your BeSpoke account",
                "Hi " + user.getName() + ",\n\n"
                        + "We received a request to delete your BeSpoke account. Your"
                        + " confirmation code is: " + code + " . It expires in 10 minutes.\n\n"
                        + "If you didn't ask for this, ignore this email — nothing happens"
                        + " without the code.\n\n"
                        + "— BeSpoke",
                page("Your account-deletion confirmation code, valid for 10 minutes.",
                        "Account deletion",
                        h("Confirm it's you,", user.getName() + "."),
                        p("Enter this code to permanently delete your BeSpoke account."
                                + " This cannot be undone.")
                                + code(code, "Expires in 10 minutes")
                                + note("If you didn't ask for this, ignore this email —"
                                        + " your account stays exactly as it is.")));
    }

    /** Sent to the original address after the account row has been anonymised. */
    public void accountDeleted(String email, String name) {
        send(email, "Your BeSpoke account has been deleted",
                "Hi " + name + ",\n\n"
                        + "Your BeSpoke account and its personal data have been deleted."
                        + " Records we must keep for legal or accounting reasons (such as"
                        + " invoices) are retained without your login details.\n\n"
                        + "— BeSpoke",
                page("Your account has been deleted.",
                        "Account deleted",
                        h("Goodbye,", name + "."),
                        p("Your account and personal data are gone. Records we must keep for"
                                + " legal or accounting reasons are retained without your"
                                + " login details. You're welcome back any time.")));
    }

    public void passwordChanged(User user) {
        send(user.getEmail(), "Your BeSpoke password was changed",
                "Hi " + user.getName() + ",\n\n"
                        + "Your password has just been changed. If this wasn't you, reply to"
                        + " this email straight away.\n\n"
                        + "— BeSpoke",
                page("Your password was just changed.",
                        "Security",
                        h("Your password", "was changed."),
                        p("The password on your BeSpoke account was changed just now.")
                                + note("If that wasn't you, write to contact@bespokedesign.in"
                                        + " straight away and we'll lock the account.")));
    }

    /** Called when the customer submits their project brief (requirement form). */
    public void briefSubmitted(User customer) {
        notifyInApp(customer, "Brief submitted", "Your requirements are ready for studio review.", "/my/requirements");
        send(customer.getEmail(), "Thanks — your project brief is in",
                "Hi " + customer.getName() + ",\n\n"
                        + "Your project brief has been submitted.\n"
                        + "Your designer will review the brief and reach out shortly.\n\n"
                        + "— BeSpoke",
                page("Your brief is in — your designer is reading it now.",
                        "Project brief",
                        h("Thanks — your brief", "is in."),
                        p("We've got everything you told us about your home. Your designer is"
                                + " reading through it now and will reach out shortly with first"
                                + " thoughts.")
                                + button(appUrl + "/my", "Open my project")));
    }

    public void staffAccountCreated(User staff, String password) {
        String where = staff.getCompany() != null ? " at " + staff.getCompany().getName() : "";
        send(staff.getEmail(), "Your BeSpoke account",
                "Hi " + staff.getName() + ",\n\n"
                        + "An account has been created for you" + where
                        + " as " + staff.getRole().name() + ".\n\n"
                        + "Email: " + staff.getEmail() + "\n"
                        + "Password: " + password + "\n\n"
                        + "Sign in: " + appUrl + "/login\n\n"
                        + "— BeSpoke",
                page("Your BeSpoke workspace account and sign-in details.",
                        "Your account",
                        h("Welcome aboard,", staff.getName() + "."),
                        p("An account has been created for you" + esc(where) + " as "
                                + esc(staff.getRole().name().replace('_', ' ').toLowerCase()) + ".")
                                + facts("Email", staff.getEmail(), "Password", password)
                                + button(appUrl + "/login", "Sign in")
                                + note("Change this password once you're in — it was generated for you.")));
    }

    public void companyOnboarded(Company company, User director, String password) {
        send(director.getEmail(), company.getName() + " is onboarded on BeSpoke",
                "Hi " + director.getName() + ",\n\n"
                        + company.getName() + " has been onboarded on BeSpoke and you are its director.\n\n"
                        + "Email: " + director.getEmail() + "\n"
                        + "Password: " + password + "\n\n"
                        + "Sign in: " + appUrl + "/login\n\n"
                        + "Complete your KYC to start receiving leads.\n\n"
                        + "— BeSpoke",
                page(company.getName() + " is on BeSpoke — finish KYC to start receiving leads.",
                        "Studio onboarded",
                        h(company.getName(), "is on BeSpoke."),
                        p("You're set up as its director. Here are your sign-in details.")
                                + facts("Email", director.getEmail(), "Password", password)
                                + p("One thing stands between you and your first lead: KYC. Complete it"
                                        + " and BeSpoke can start routing projects to you.")
                                + button(appUrl + "/login", "Sign in and finish KYC")));
    }

    /**
     * Sent the moment the partner form is submitted, so nobody is left wondering whether it
     * went anywhere. Deliberately promises only a review — the account does not exist yet
     * and there is nothing to sign in to until an admin approves.
     */
    public void partnerApplicationReceived(PartnerApplication application) {
        String firstName = application.getContactName().split("\\s+")[0];
        send(application.getContactEmail(),
                "We've got your application — BeSpoke Partners",
                "Hi " + firstName + ",\n\n"
                        + "Thanks for applying to join BeSpoke with " + application.getCompanyName()
                        + ". Your application is with our team.\n\n"
                        + "Business: " + application.getCompanyName() + "\n"
                        + "City: " + application.getCity() + "\n"
                        + "Email: " + application.getContactEmail() + "\n"
                        + "Phone: " + (application.getContactPhone() == null
                                ? "—" : application.getContactPhone()) + "\n\n"
                        + "We verify every partner by hand. As soon as you're approved we'll"
                        + " email you here with your sign-in details and your workspace will"
                        + " be open — there's nothing to do until then.\n\n"
                        + "— BeSpoke",
                page("Your partner application is with our team — we'll be in touch shortly.",
                        "Application received",
                        h("Thanks for applying,", firstName + "."),
                        p("We've got " + esc(application.getCompanyName()) + "'s application"
                                + " and our team is reviewing it now. Here's what you sent us.")
                                + facts("Business", application.getCompanyName(),
                                        "City", application.getCity(),
                                        "Email", application.getContactEmail(),
                                        "Phone", application.getContactPhone() == null
                                                ? "—" : application.getContactPhone())
                                + p("We verify every partner by hand, so this takes a little"
                                        + " time. The moment you're approved we'll email you"
                                        + " here with your sign-in details and your workspace"
                                        + " will be open.")
                                + note("Nothing to do until then — your account is created"
                                        + " when the application is approved, not before.")));
    }

    /**
     * The same event, told to us. Goes to contact@ so a new partner shows up somewhere a
     * human already looks, instead of only in the admin queue nobody refreshes. Carries
     * every field the applicant gave plus a deep link straight to the decision screen.
     */
    public void partnerApplicationInternal(PartnerApplication application) {
        String queue = appUrl + "/admin/applications";
        send(internalTo,
                "New partner signup: " + application.getCompanyName()
                        + " (" + application.getCity() + ")",
                "A new partner application has come in.\n\n"
                        + "Business: " + application.getCompanyName() + "\n"
                        + "City: " + application.getCity() + "\n"
                        + "Contact: " + application.getContactName() + "\n"
                        + "Email: " + application.getContactEmail() + "\n"
                        + "Phone: " + (application.getContactPhone() == null
                                ? "—" : application.getContactPhone()) + "\n"
                        + "Reference: APP-" + application.getId() + "\n\n"
                        + "Approve or decline: " + queue + "\n\n"
                        + "— BeSpoke",
                page(application.getCompanyName() + " applied to join BeSpoke.",
                        "New signup",
                        h("A new partner", "applied."),
                        p(esc(application.getCompanyName()) + " has applied to join BeSpoke."
                                + " Nothing exists on the platform yet — approving is what"
                                + " creates the company and its director account.")
                                + facts("Business", application.getCompanyName(),
                                        "City", application.getCity(),
                                        "Contact", application.getContactName(),
                                        "Email", application.getContactEmail(),
                                        "Phone", application.getContactPhone() == null
                                                ? "—" : application.getContactPhone(),
                                        "Reference", "APP-" + application.getId())
                                + button(queue, "Review the application")));
    }

    /**
     * The one mail a partner waits for: their application was approved and their workspace
     * exists. Carries the first-time password the admin set — this is the only place it is
     * ever shown, so it reads as a welcome, not a receipt.
     */
    public void partnerApproved(Company company, User director, String password) {
        String signIn = appUrl + "/partner/login";
        boolean vendor = company.getType() == CompanyType.VENDOR;
        String firstName = director.getName().split("\\s+")[0];
        send(director.getEmail(),
                "Welcome to BeSpoke, " + company.getName() + " — your workspace is open",
                "Hi " + firstName + ",\n\n"
                        + "Good news: " + company.getName() + " has been approved to join BeSpoke,"
                        + " and your workspace is open. You're set up as its director.\n\n"
                        + "Email: " + director.getEmail() + "\n"
                        + "Password: " + password + "\n\n"
                        + "Sign in: " + signIn + "\n\n"
                        + "First three things to do:\n"
                        + "1. Sign in and change this password.\n"
                        + "2. Finish KYC — registration, GST and identity documents.\n"
                        + "3. Complete your public profile so you appear in the directory.\n\n"
                        + (vendor
                                ? "Once you're verified, your catalogue can go live on the shop.\n\n"
                                : "Once you're verified, briefed leads start reaching you.\n\n")
                        + "— BeSpoke",
                page(company.getName() + " is approved — your BeSpoke workspace is open.",
                        "Application approved",
                        h("Welcome to BeSpoke,", company.getName() + "."),
                        p("Hi " + esc(firstName) + " — your application came through our team and"
                                + " it's a yes. " + esc(company.getName()) + " is on the platform"
                                + " and you're set up as its director. Here's how you get in.")
                                + facts("Email", director.getEmail(), "Password", password)
                                + button(signIn, "Open your workspace")
                                + p("<strong>Three things to do first:</strong>")
                                + facts(
                                        "Step 01", "Sign in and change this password",
                                        "Step 02", "Finish KYC — registration, GST and ID",
                                        "Step 03", "Complete your profile so you're listed")
                                + p(vendor
                                        ? "The moment you're verified, your catalogue can go live"
                                                + " on the BeSpoke shop and orders start landing in"
                                                + " one queue."
                                        : "The moment you're verified, BeSpoke starts routing you"
                                                + " briefed leads — rooms, budget, timeline and city,"
                                                + " already scored.")
                                + note("This password was set for you by the BeSpoke team."
                                        + " Change it as soon as you're in.")));
    }

    /**
     * Acknowledgement to the person behind a brand-new lead, whichever door they came
     * through (enquiry form, staff capture, mail to contact@). {@code inReplyToSubject}
     * is the subject of the mail we are answering — set it only for the contact@ inbox
     * path so the acknowledgement reads as a reply; null everywhere else.
     */
    public void leadReceived(Lead lead, String inReplyToSubject) {
        String subject = "We've got your enquiry — BeSpoke";
        if (inReplyToSubject != null && !inReplyToSubject.isBlank()) {
            String trimmed = inReplyToSubject.trim();
            subject = trimmed.toLowerCase().startsWith("re:") ? trimmed : "Re: " + trimmed;
        }
        send(lead.getContactEmail(), subject,
                "Hi " + lead.getContactName() + ",\n\n"
                        + "Thanks for reaching out to BeSpoke. Your enquiry is with our team and"
                        + " one of our designers will be in touch shortly.\n\n"
                        + "Your reference: BSD-" + lead.getId() + "\n\n"
                        + "This mailbox is not monitored — reply to contact@bespokedesign.in if you"
                        + " have anything to add.\n\n"
                        + "— BeSpoke",
                page("We've got your enquiry — a designer will be in touch shortly.",
                        "Enquiry received",
                        h("Thanks for reaching out,", lead.getContactName() + "."),
                        p("Your enquiry is with our team. One of our designers will be in touch"
                                + " shortly to understand what you have in mind.")
                                + facts("Your reference", "BSD-" + lead.getId())
                                + note("This mailbox isn't monitored — write to"
                                        + " contact@bespokedesign.in if you have anything to add.")));
    }

    /**
     * Tells the shared mailbox a lead arrived — whatever the source: website signup,
     * enquiry form, walk-in typed into the CRM, or a mail into contact@ itself. The
     * customer's own acknowledgement is {@link #leadReceived}; this is the internal copy,
     * so nothing lands in the funnel without a human being told.
     */
    public void leadReceivedInternal(Lead lead, String source) {
        String name = lead.getContactName() == null ? "Someone" : lead.getContactName();
        send(internalTo, "New lead: " + name + " (" + source + ")",
                "A new lead is in the funnel.\n\n"
                        + "Name: " + name + "\n"
                        + "Email: " + orDash(lead.getContactEmail()) + "\n"
                        + "Phone: " + orDash(lead.getContactPhone()) + "\n"
                        + "City: " + orDash(lead.getCity()) + "\n"
                        + "Source: " + source + "\n"
                        + "Reference: BSD-" + lead.getId() + "\n\n"
                        + appUrl + "/studio/leads/" + lead.getId(),
                page("A new lead is in the funnel.", "New lead",
                        h("New lead —", name),
                        facts("Email", orDash(lead.getContactEmail()),
                                "Phone", orDash(lead.getContactPhone()),
                                "City", orDash(lead.getCity()),
                                "Source", source,
                                "Reference", "BSD-" + lead.getId())
                                + button(appUrl + "/studio/leads/" + lead.getId(), "Open the lead")));
    }

    private static String orDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    /** Phase two is ready for the customer to read and sign off. */
    public void prdSentForReview(User customer, int spaces) {
        send(customer.getEmail(), "Your project requirement document is ready to review",
                "Hi " + customer.getName() + ",\n\n"
                        + "Your designer has put together the project requirement document — "
                        + spaces + " space" + (spaces == 1 ? "" : "s") + ", each with its size"
                        + " and what goes into it.\n\n"
                        + "Read it and approve it here: " + appUrl + "/my/requirements\n\n"
                        + "— BeSpoke",
                page("Your project requirement document is ready to review.", "PRD ready",
                        h("Ready for your sign-off,", customer.getName() + "."),
                        p("Your designer has put together the project requirement document — "
                                + spaces + " space" + (spaces == 1 ? "" : "s") + ", each with its"
                                + " size and what goes into it. Nothing is ordered or drawn"
                                + " against it until you approve it.")
                                + button(appUrl + "/my/requirements", "Read the PRD")));
        notifyInApp(customer, "Your PRD is ready to review",
                spaces + " space" + (spaces == 1 ? "" : "s") + " to read and approve.",
                "/my/requirements");
    }

    /** An instalment of the agreed payment schedule has fallen due. */
    public void paymentDue(User customer, String title, java.math.BigDecimal amount) {
        String rupees = "Rs " + amount.stripTrailingZeros().toPlainString();
        send(customer.getEmail(), "Payment due — " + title,
                "Hi " + customer.getName() + ",\n\n"
                        + title + ": " + rupees + " is now due under your agreed payment"
                        + " schedule.\n\n"
                        + "See it here: " + appUrl + "/my/payments\n\n— BeSpoke",
                page(title + ": " + rupees + " is now due.", "Payment due",
                        h("Payment due,", customer.getName() + "."),
                        p(title + " has fallen due under the payment schedule you agreed.")
                                + facts("Amount", rupees, "For", title)
                                + button(appUrl + "/my/payments", "View and pay")));
        notifyInApp(customer, "Payment due — " + title, rupees + " is now due.", "/my/payments");
    }

    /** Sent to the receiving studio's director and sales manager when BeSpoke routes a lead. */
    public void leadRouted(User recipient, Lead lead) {
        String property = label("PROPERTY_TYPE", lead.getPropertyType());
        String budget = label("BUDGET_BAND", lead.getBudgetBand());
        send(recipient.getEmail(), "New lead from BeSpoke: " + lead.getContactName(),
                "Hi " + recipient.getName() + ",\n\n"
                        + "BeSpoke has routed a new project enquiry to you.\n\n"
                        + "Customer: " + lead.getContactName() + "\n"
                        + "City: " + lead.getCity() + "\n"
                        + "Property: " + property + "\n"
                        + "Budget: " + budget + "\n\n"
                        + "Accept it in your dashboard: " + appUrl + "/studio/leads/" + lead.getId() + "\n\n"
                        + "— BeSpoke",
                page("A new project enquiry has been routed to your studio.",
                        "New lead",
                        h("A new project", "for you."),
                        p("BeSpoke has routed a project enquiry to your studio. Accept it to start"
                                + " working — until you do, it stays in the pool.")
                                + facts("Customer", lead.getContactName(),
                                        "City", lead.getCity(),
                                        "Property", property,
                                        "Budget", budget)
                                + button(appUrl + "/studio/leads/" + lead.getId(), "Review and accept")));
    }

    public void leadAccepted(User customer, String studioName) {
        notifyInApp(customer, "Your studio is ready", studioName + " has accepted your project.", "/my");
        send(customer.getEmail(), studioName + " has accepted your project",
                "Hi " + customer.getName() + ",\n\n"
                        + studioName + " has accepted your project and will be in touch shortly.\n"
                        + "You can now message them from your dashboard: " + appUrl + "/my\n\n"
                        + "— BeSpoke",
                page(studioName + " has accepted your project.",
                        "Good news",
                        h(studioName, "accepted your project."),
                        p("They'll be in touch shortly. From now on you can message them directly"
                                + " and follow every drawing, quote and milestone in one place.")
                                + button(appUrl + "/my", "Open my project")));
    }

    /**
     * "Your quote is ready." Addressed to the account when there is one and to the lead's
     * contact address when there isn't — a quote can go out before the customer signs up.
     */
    public void quoteSent(Lead lead, String title, int version) {
        notifyInApp(lead.getCustomer(), "Proposal ready to review", title + " · version " + version, "/my/proposals");
        send(lead.getCustomer() != null ? lead.getCustomer().getEmail() : lead.getContactEmail(),
                "Your BeSpoke proposal is ready",
                "Hi " + lead.getContactName() + ",\n\n"
                        + "Proposal v" + version + " — \"" + title + "\" — is ready for you"
                        + " to review.\n"
                        + appUrl + "/my/proposals\n\n"
                        + "— BeSpoke",
                page("Your proposal is ready to review.",
                        "Proposal",
                        h("Your proposal", "is ready."),
                        p("Your designer has put together the details — scope, materials and costing"
                                + " — for you to look over.")
                                + facts("Proposal", title, "Version", "v" + version)
                                + button(appUrl + "/my/proposals", "Review the proposal")));
    }

    public void invoiceSent(User customer, String number, String amount, Object dueDate) {
        notifyInApp(customer, "New invoice", "Invoice " + number + " is ready. View your payment ledger for details.", "/my/payments");
        send(customer.getEmail(), "Invoice " + number + " from BeSpoke",
                "Hi " + customer.getName() + ",\n\n"
                        + "Invoice " + number + " for ₹" + amount + " is ready"
                        + (dueDate != null ? ", due " + dueDate : "") + ".\n"
                        + appUrl + "/my/payments\n\n"
                        + "— BeSpoke",
                page("Invoice " + number + " is ready.",
                        "Invoice",
                        h("Invoice", number),
                        p("This invoice is now due on your project. You can view the breakdown and"
                                + " every payment recorded against it in your dashboard.")
                                + facts("Invoice", number,
                                        "Amount", "₹ " + amount,
                                        "Due", dueDate != null ? String.valueOf(dueDate) : "On receipt")
                                + button(appUrl + "/my/payments", "View invoice")));
    }

    public void paymentReceived(User customer, String number, String amount, String outstanding) {
        notifyInApp(customer, "Payment received",
                "₹" + amount + " received against invoice " + number + ".", "/my/payments");
        send(customer.getEmail(), "Payment received — invoice " + number,
                "Hi " + customer.getName() + ",\n\n"
                        + "We've recorded a payment of ₹" + amount + " against invoice " + number + ".\n"
                        + "Outstanding balance: ₹" + outstanding + ".\n"
                        + appUrl + "/my/payments\n\n"
                        + "— BeSpoke",
                page("Payment of ₹" + amount + " received.",
                        "Payment received",
                        h("Payment", "received"),
                        p("Thank you — your payment has been recorded. Your ledger reflects it"
                                + " already, along with anything still outstanding.")
                                + facts("Invoice", number,
                                        "Received", "₹ " + amount,
                                        "Outstanding", "₹ " + outstanding)
                                + button(appUrl + "/my/payments", "View payment ledger")));
    }

    public void orderPlaced(User customer, String vendorName, String total) {
        notifyInApp(customer, "Order placed", "Your order with " + vendorName + " has been placed.", "/my/orders");
        send(customer.getEmail(), "Your BeSpoke order is confirmed",
                "Hi " + customer.getName() + ",\n\n"
                        + "Your order with " + vendorName + " for ₹" + total + " is confirmed."
                        + " The vendor will confirm dispatch shortly.\n"
                        + appUrl + "/my/orders\n\n"
                        + "— BeSpoke",
                page("Your order is confirmed.",
                        "Order confirmed",
                        h("Your order", "is confirmed."),
                        p("Thanks for your order. " + esc(vendorName) + " will confirm dispatch"
                                + " shortly, and you'll see the status change in your dashboard.")
                                + facts("Vendor", vendorName, "Order total", "₹ " + total)
                                + button(appUrl + "/my/orders", "Track my order")));
    }

    /** KYC is the gate on receiving leads, so the director hears about either outcome. */
    public void kycDecision(User director, Company company, boolean verified) {
        send(director.getEmail(),
                verified ? company.getName() + " is verified on BeSpoke"
                        : "KYC needs another look — " + company.getName(),
                "Hi " + director.getName() + ",\n\n"
                        + (verified
                                ? company.getName() + " is KYC-verified and can now receive leads."
                                : "We could not verify " + company.getName() + " with the documents"
                                        + " on file. Please review and resubmit them.")
                        + "\n" + appUrl + "/studio/profile\n\n"
                        + "— BeSpoke",
                verified
                        ? page(company.getName() + " is verified and can receive leads.",
                                "KYC verified",
                                h(company.getName(), "is verified."),
                                p("Your documents check out. From now on BeSpoke can route projects"
                                        + " to your studio — make sure your public profile is complete"
                                        + " so customers see you at your best.")
                                        + button(appUrl + "/studio/profile", "Review my profile"))
                        : page("We couldn't verify your documents — please resubmit.",
                                "KYC review",
                                h("Your KYC needs", "another look."),
                                p("We couldn't verify " + esc(company.getName()) + " with the documents"
                                        + " currently on file. Please check them and resubmit.")
                                        + button(appUrl + "/studio/profile", "Update documents")
                                        + note("Not sure what's missing? Reply to"
                                                + " contact@bespokedesign.in and we'll walk you through it.")));
    }

    /**
     * Somebody tagged you on a task. Sent to the assignee only — the person who wrote it
     * already knows — and skipped entirely when you assign work to yourself.
     */
    public void taskAssigned(StaffTask task) {
        String workUrl = (task.getCompany().getType() == com.BeSpoke.entity.CompanyType.VENDOR
                ? "/vendor/work" : "/studio/work") + "?task=" + task.getId();
        notifyInApp(task.getAssignee(), "New task assigned", task.getTitle(), workUrl);
        User assignee = task.getAssignee();
        String from = task.getCreatedBy().getName();
        String due = task.getDueDate() == null ? "No date set" : String.valueOf(task.getDueDate());
        send(assignee.getEmail(),
                from + " assigned you a task: " + task.getTitle(),
                "Hi " + assignee.getName() + ",\n\n"
                        + from + " has assigned you a task on BeSpoke.\n\n"
                        + "Task: " + task.getTitle() + "\n"
                        + (task.getDetails() == null || task.getDetails().isBlank()
                                ? "" : "Details: " + task.getDetails() + "\n")
                        + "Due: " + due + "\n\n"
                        + appUrl + workUrl + "\n\n"
                        + "— BeSpoke",
                page(from + " assigned you a task — " + task.getTitle(),
                        "New task",
                        h("You've been", "tagged."),
                        p("<strong>" + esc(from) + "</strong> has assigned you a task.")
                                + facts("Task", task.getTitle(),
                                        "Details", task.getDetails() == null
                                                || task.getDetails().isBlank()
                                                ? "—" : task.getDetails(),
                                        "Due", due,
                                        "From", from)
                                + button(appUrl + workUrl, "Open my tasks")));
    }

    /** Called by DrawingService when a drawing is approved (V3 §5). */
    public void drawingApproved(User customer, String title) {
        // DrawingService persists the in-app notification with the design transaction.
        send(customer.getEmail(), "A new design is ready to view",
                "Hi " + customer.getName() + ",\n\n"
                        + "\"" + title + "\" has been approved and is ready to view.\n"
                        + appUrl + "/my/designs\n\n"
                        + "— BeSpoke",
                page("A new design is ready for you to see.",
                        "New design",
                        h("Your design is", "ready to view."),
                        p("“" + esc(title) + "” has been approved by the design team and is"
                                + " waiting for you.")
                                + button(appUrl + "/my/designs", "View the design")));
    }
}
