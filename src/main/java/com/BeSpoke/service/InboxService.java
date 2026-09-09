package com.BeSpoke.service;

import com.BeSpoke.entity.Lead;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.search.FlagTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Properties;

/**
 * Reads contact@bespokedesign.in over IMAP and turns each unread mail into a lead.
 * Replies never come from here: {@link LeadService#createFromEmail} acknowledges via
 * {@link MailService}, which always sends as noreply@ (app.mail.from). contact@ is
 * read-only to us — nothing is ever sent with it as the From address.
 *
 * <p>The IMAP \Seen flag is the dedupe record: a message is flagged after we have
 * looked at it, so a restart never re-processes it and no DB bookkeeping is needed.
 */
@Service
public class InboxService {

    private static final Logger log = LoggerFactory.getLogger(InboxService.class);

    private final LeadService leadService;
    private final boolean enabled;
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String sendAs;
    private final long pollMs;
    private final boolean selfPoll;

    public InboxService(LeadService leadService,
                        @Value("${app.inbox.enabled:false}") boolean enabled,
                        @Value("${app.inbox.host:imap.gmail.com}") String host,
                        @Value("${app.inbox.port:993}") int port,
                        @Value("${app.inbox.username:}") String username,
                        @Value("${app.inbox.password:}") String password,
                        @Value("${app.inbox.poll-ms:120000}") long pollMs,
                        @Value("${app.mail.from:}") String sendAs) {
        this.leadService = leadService;
        this.enabled = enabled;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.sendAs = sendAs;
        this.pollMs = pollMs;
        // Polling the mailbox we send FROM turns every bounce and provider notice into a
        // lead we then answer — a loop with our own name on both ends. It happened in
        // production: INBOX_USERNAME pointed at noreply@ and Google's security alert came
        // back "Hi Google, thanks for reaching out". Refuse rather than obey.
        this.selfPoll = !username.isBlank() && sendAs.toLowerCase(Locale.ROOT)
                .contains(username.toLowerCase(Locale.ROOT));
    }

    /** Same reasoning as MailService: a mailbox nobody is reading must not look like silence. */
    @EventListener(ApplicationReadyEvent.class)
    public void reportInboxState() {
        if (!enabled) {
            log.info("[INBOX] disabled — mail to {} will not become leads (set INBOX_ENABLED=true)",
                    username.isBlank() ? "contact@" : username);
        } else if (username.isBlank() || password.isBlank()) {
            log.error("[INBOX] enabled but username/password missing — contact@ will never be read.");
        } else if (selfPoll) {
            log.error("[INBOX] REFUSING to poll {} — it is the address we send from ({})."
                    + " Set INBOX_USERNAME to the inbound mailbox (contact@bespokedesign.in).",
                    username, sendAs);
        } else {
            log.info("[INBOX] enabled — polling {} every {}ms", username, pollMs);
        }
    }

    @Scheduled(fixedDelayString = "${app.inbox.poll-ms:120000}", initialDelayString = "${app.inbox.initial-delay-ms:30000}")
    public void poll() {
        if (!enabled || selfPoll || username.isBlank() || password.isBlank()) {
            return;
        }
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.ssl.enable", "true");
        Store store = null;
        Folder inbox = null;
        try {
            store = Session.getInstance(props).getStore("imaps");
            store.connect(host, port, username, password);
            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);
            Message[] unread = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            // DEBUG on a quiet poll, INFO when there is work: the usual poll is silent in
            // prod, but raising this logger proves the schedule is alive without waiting
            // for someone to write in.
            log.atLevel(unread.length == 0 ? org.slf4j.event.Level.DEBUG : org.slf4j.event.Level.INFO)
                    .log("[INBOX] {} unread in {}", unread.length, username);
            for (Message message : unread) {
                try {
                    handle(message);
                } catch (Exception ex) {
                    log.warn("[INBOX] could not process a message: {}", ex.toString());
                } finally {
                    // ponytail: flag either way, so one poison message can't wedge the poll
                    // loop. It stays in contact@ unread-in-spirit only — grep this warning.
                    message.setFlag(Flags.Flag.SEEN, true);
                }
            }
        } catch (Exception ex) {
            log.warn("[INBOX] poll of {} failed: {}", username, ex.toString());
        } finally {
            close(inbox);
            close(store);
        }
    }

    private void handle(Message message) throws Exception {
        InternetAddress sender = message.getFrom() == null || message.getFrom().length == 0
                ? null : (InternetAddress) message.getFrom()[0];
        if (sender == null || sender.getAddress() == null) {
            return;
        }
        String from = sender.getAddress().toLowerCase(Locale.ROOT);
        if (isMachineMail(message, from)) {
            log.info("[INBOX] skipping automated mail from {}", from);
            return;
        }
        Lead lead = leadService.createFromEmail(
                from, sender.getPersonal(), message.getSubject(), textOf(message));
        log.info("[INBOX] mail from {} → lead #{}", from, lead.getId());
    }

    /**
     * Our own notifications, bounces and vacation autoresponders must not become leads —
     * and must not be replied to, which is how mail loops start.
     */
    private boolean isMachineMail(Message message, String from) throws Exception {
        if (isMachineAddress(from)) {
            return true;
        }
        String[] autoSubmitted = message.getHeader("Auto-Submitted");
        if (autoSubmitted != null && autoSubmitted.length > 0
                && !"no".equalsIgnoreCase(autoSubmitted[0].trim())) {
            return true;
        }
        String[] autoReply = message.getHeader("X-Autoreply");
        return autoReply != null && autoReply.length > 0;
    }

    /**
     * Addresses no human reads. Live proof this is needed: Google's own
     * "Security alert" from no-reply@accounts.google.com became a lead and got
     * answered with "Hi Google, thanks for reaching out" — because the only rule
     * here used to be our own domain. Any no-reply sender is a machine.
     */
    static boolean isMachineAddress(String from) {
        if (from == null || from.isBlank() || from.endsWith("@bespokedesign.in")) {
            return true;
        }
        String local = from.substring(0, Math.max(from.indexOf('@'), 0)).replace(".", "").replace("-", "");
        return local.startsWith("noreply") || local.startsWith("donotreply")
                || local.equals("mailerdaemon") || local.equals("postmaster")
                || local.startsWith("bounce");
    }

    /**
     * First readable text in the message. Prefers text/plain — in a multipart/alternative
     * that is the part Gmail puts first — and falls back to tag-stripped HTML.
     */
    static String textOf(Part part) throws Exception {
        // Branch on what getContent() actually hands back, not on the Content-Type header:
        // a mail with a missing or lying header would otherwise file an empty lead.
        Object content = part.getContent();
        if (content instanceof Multipart multipart) {
            String html = "";
            for (int i = 0; i < multipart.getCount(); i++) {
                Part child = multipart.getBodyPart(i);
                String text = textOf(child);
                if (text.isBlank()) {
                    continue;
                }
                if (child.isMimeType("text/html")) {
                    html = html.isBlank() ? text : html;   // keep as fallback, keep looking
                } else {
                    return text;
                }
            }
            return html;
        }
        if (content instanceof String text) {
            return part.isMimeType("text/html") ? stripTags(text) : text.trim();
        }
        return "";  // attachments and anything else we cannot read as text
    }

    static String stripTags(String html) {
        return html.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ")
                .replaceAll("(?s)<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\n\\s*\n\\s*\n+", "\n\n")
                .trim();
    }

    private static void close(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
            // a half-closed IMAP connection is the server's problem, not ours
        }
    }
}
