package com.BeSpoke.service;

import com.BeSpoke.dto.PlatformOptionDto;
import com.BeSpoke.entity.ActivityType;
import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.LeadActivity;
import com.BeSpoke.entity.User;
import com.BeSpoke.repository.LeadActivityRepository;
import com.BeSpoke.repository.LeadRepository;
import com.BeSpoke.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * WhatsApp over Gupshup. Two kinds of message, and WhatsApp's rules decide which:
 *
 * <ul>
 *   <li><b>Template</b> — anything BeSpoke starts (the sign-in code, the welcome after
 *       signup). Meta must approve the wording first; the id of the approved template
 *       is configuration, never text we write here.</li>
 *   <li><b>Session</b> — a reply inside the 24 hours after the customer last wrote to
 *       us. Free text, which is what lets the bot answer in its own words.</li>
 * </ul>
 *
 * <p>Disabled ({@code app.whatsapp.enabled=false}, the default) the whole message is
 * logged behind a {@code [WHATSAPP]} prefix instead of being sent — the same bargain
 * {@link MailService} strikes, so a developer reads codes off the console. A send
 * failure is always swallowed: a notification may never fail the request behind it.
 */
@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    private static final URI SESSION_URL = URI.create("https://api.gupshup.io/wa/api/v1/msg");
    private static final URI TEMPLATE_URL = URI.create("https://api.gupshup.io/wa/api/v1/template/msg");

    /** The admin-owned reply book. `value` = keywords, `label` = what the bot says. */
    public static final String BOT_LIST = "WHATSAPP_BOT";
    /** The rule that answers anything unmatched — where the "call us" number lives. */
    public static final String FALLBACK_KEY = "*";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    /** Lazy, like MailService's: this service is wired into the auth path. */
    private final ObjectProvider<PlatformOptionService> options;
    private final UserRepository userRepository;
    private final LeadRepository leadRepository;
    private final LeadActivityRepository leadActivityRepository;

    private final boolean enabled;
    private final String apiKey;
    private final String source;
    private final String appName;
    private final String otpTemplateId;
    private final String welcomeTemplateId;
    private final String countryCode;

    public WhatsAppService(ObjectProvider<PlatformOptionService> options,
                           UserRepository userRepository,
                           LeadRepository leadRepository,
                           LeadActivityRepository leadActivityRepository,
                           @Value("${app.whatsapp.enabled:false}") boolean enabled,
                           @Value("${app.whatsapp.api-key:}") String apiKey,
                           @Value("${app.whatsapp.source:}") String source,
                           @Value("${app.whatsapp.app-name:}") String appName,
                           @Value("${app.whatsapp.otp-template-id:}") String otpTemplateId,
                           @Value("${app.whatsapp.welcome-template-id:}") String welcomeTemplateId,
                           @Value("${app.whatsapp.country-code:91}") String countryCode) {
        this.options = options;
        this.userRepository = userRepository;
        this.leadRepository = leadRepository;
        this.leadActivityRepository = leadActivityRepository;
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.source = source;
        this.appName = appName;
        this.otpTemplateId = otpTemplateId;
        this.welcomeTemplateId = welcomeTemplateId;
        this.countryCode = countryCode;
    }

    // ---- outbound ----

    /** The same six digits the email carries — one code, two ways to reach it. */
    public void otp(User user, String code) {
        template(user.getPhone(), otpTemplateId, List.of(code), "sign-in code for " + user.getEmail());
    }

    /** Signup done: the opener that starts the conversation and opens the 24-hour window. */
    public void welcome(User user) {
        template(user.getPhone(), welcomeTemplateId, List.of(firstName(user.getName())),
                "welcome to " + user.getEmail());
    }

    /**
     * An inbound WhatsApp message: answer it from the admin's reply book, and file both
     * sides on the customer's lead so the studio sees the conversation in the CRM.
     */
    public void handleInbound(String fromMsisdn, String senderName, String text) {
        String answer = replyFor(text, botRules());
        log.info("[WHATSAPP] in from {} ({}): {}", fromMsisdn, senderName, text);
        session(fromMsisdn, answer);
        record(fromMsisdn, text, answer);
    }

    /** Free text inside the 24-hour window. Public so a human reply can use it too. */
    public void session(String phone, String text) {
        String to = msisdn(phone, countryCode);
        if (to == null || text == null || text.isBlank()) {
            return;
        }
        if (!ready()) {
            log.info("[WHATSAPP] (not sent — disabled) to {}: {}", to, text);
            return;
        }
        post(SESSION_URL, form(to) + "&message=" + enc("{\"type\":\"text\",\"text\":" + json(text) + "}"),
                "message to " + to);
    }

    private void template(String phone, String templateId, List<String> params, String what) {
        String to = msisdn(phone, countryCode);
        if (to == null) {
            log.warn("[WHATSAPP] no usable number for {} — nothing sent", what);
            return;
        }
        if (!ready() || templateId.isBlank()) {
            log.info("[WHATSAPP] (not sent — {}) {} to {}: {}",
                    enabled ? "no template id" : "disabled", what, to, params);
            return;
        }
        String body = form(to) + "&template="
                + enc("{\"id\":" + json(templateId) + ",\"params\":["
                        + params.stream().map(WhatsAppService::json).reduce((a, b) -> a + "," + b).orElse("")
                        + "]}");
        post(TEMPLATE_URL, body, what);
    }

    private boolean ready() {
        return enabled && !apiKey.isBlank() && !source.isBlank();
    }

    private String form(String destination) {
        return "channel=whatsapp&source=" + enc(source)
                + "&destination=" + enc(destination)
                + "&src.name=" + enc(appName);
    }

    private void post(URI url, String body, String what) {
        try {
            HttpResponse<String> response = http.send(
                    HttpRequest.newBuilder(url)
                            .header("apikey", apiKey)
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .timeout(Duration.ofSeconds(15))
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                // Gupshup answers 4xx with a readable reason — an unapproved template,
                // an unfunded wallet, a number that never opted in. Worth the log line.
                log.warn("[WHATSAPP] {} rejected ({}): {}", what, response.statusCode(), response.body());
            } else {
                log.info("[WHATSAPP] sent {}", what);
            }
        } catch (Exception ex) {
            log.warn("[WHATSAPP] {} failed: {}", what, ex.toString());
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ---- the bot ----

    private List<PlatformOptionDto> botRules() {
        PlatformOptionService service = options.getIfAvailable();
        if (service == null) {
            return List.of();
        }
        return service.publicLists().getOrDefault(BOT_LIST, List.of());
    }

    /**
     * First rule whose keywords appear in the message wins; the {@code *} rule answers
     * everything else — that is the one that says to call, and it is why an unmatched
     * message is never met with silence. Rules are the admin's, edited at /admin/options.
     */
    static String replyFor(String text, List<PlatformOptionDto> rules) {
        String message = text == null ? "" : text.toLowerCase(Locale.ROOT);
        String fallback = null;
        for (PlatformOptionDto rule : rules) {
            if (FALLBACK_KEY.equals(rule.value().trim())) {
                if (fallback == null) {
                    fallback = rule.label();
                }
                continue;
            }
            for (String keyword : rule.value().split(",")) {
                String needle = keyword.trim().toLowerCase(Locale.ROOT);
                if (!needle.isEmpty() && message.contains(needle)) {
                    return rule.label();
                }
            }
        }
        return fallback;
    }

    /** Files both halves of the exchange on the customer's newest lead, when we know them. */
    private void record(String fromMsisdn, String inbound, String answer) {
        Optional<User> customer = byPhone(fromMsisdn);
        if (customer.isEmpty()) {
            return;
        }
        List<Lead> leads = leadRepository.findByCustomerOrderByCreatedAtDesc(customer.get());
        if (leads.isEmpty()) {
            return;
        }
        Lead lead = leads.get(0);
        leadActivityRepository.save(new LeadActivity(lead, null, ActivityType.NOTE,
                "WhatsApp from customer: " + inbound));
        if (answer != null && !answer.isBlank()) {
            leadActivityRepository.save(new LeadActivity(lead, null, ActivityType.SYSTEM,
                    "WhatsApp auto-reply: " + answer));
        }
    }

    /**
     * Numbers are stored as they were typed — "9810012345" today, "+91 98100 12345" on
     * older accounts — and WhatsApp hands us "919810012345". The last ten digits are the
     * one thing all three agree on.
     */
    private Optional<User> byPhone(String msisdn) {
        String digits = digitsOf(msisdn);
        if (digits.length() < 10) {
            return Optional.empty();
        }
        String last10 = digits.substring(digits.length() - 10);
        // ponytail: three indexed lookups, not a scan and not a normalised column. A
        // number stored with spaces ("+91 98100 12345") still misses — backfill the
        // column to digits if that ever matters more than the lookup being free.
        return userRepository.findFirstByPhone(last10)
                .or(() -> userRepository.findFirstByPhone(digits))
                .or(() -> userRepository.findFirstByPhoneEndingWith(last10));
    }

    // ---- helpers ----

    /**
     * To what Gupshup wants: digits only, country code included. A bare ten-digit Indian
     * mobile gets the configured code; anything already carrying one is left alone.
     */
    static String msisdn(String raw, String countryCode) {
        String digits = digitsOf(raw);
        if (digits.length() == 10) {
            return countryCode + digits;
        }
        // 11 digits starting 0 is the old STD form — 09810012345.
        if (digits.length() == 11 && digits.startsWith("0")) {
            return countryCode + digits.substring(1);
        }
        return digits.length() >= 11 && digits.length() <= 15 ? digits : null;
    }

    private static String digitsOf(String raw) {
        return raw == null ? "" : raw.replaceAll("\\D", "");
    }

    private static String firstName(String name) {
        if (name == null || name.isBlank()) {
            return "there";
        }
        return name.trim().split("\\s+")[0];
    }

    private static String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    /** Minimal JSON string literal — the payloads here are one field deep. */
    private static String json(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "") + "\"";
    }
}
