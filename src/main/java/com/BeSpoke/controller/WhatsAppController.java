package com.BeSpoke.controller;

import com.BeSpoke.service.WhatsAppService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gupshup's callback: every inbound WhatsApp message, plus delivery receipts we ignore.
 * Set this URL in the Gupshup dashboard (Settings → Callback URL) with the token:
 * {@code https://crm.bespokedesign.in/api/whatsapp/gupshup?token=…}.
 *
 * <p>Always answers 200 once the token checks out. A webhook that returns an error gets
 * retried, and a retry storm over a bad message helps nobody — the failure belongs in
 * our log, not in Gupshup's queue.
 */
@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppController.class);

    private final WhatsAppService whatsApp;
    private final String expectedToken;

    public WhatsAppController(WhatsAppService whatsApp,
                              @Value("${app.whatsapp.webhook-token:}") String expectedToken) {
        this.whatsApp = whatsApp;
        this.expectedToken = expectedToken;
    }

    @PostMapping("/gupshup")
    public ResponseEntity<Void> inbound(@RequestParam(required = false) String token,
                                        @RequestHeader(value = "x-bespoke-token", required = false) String header,
                                        @RequestBody JsonNode body) {
        // Unset token = the endpoint is shut, not open: an unconfigured webhook must not
        // be a way for anyone to make the bot send messages.
        if (expectedToken.isBlank()
                || !(expectedToken.equals(token) || expectedToken.equals(header))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            if ("message".equals(body.path("type").asText())) {
                JsonNode payload = body.path("payload");
                if ("text".equals(payload.path("type").asText())) {
                    whatsApp.handleInbound(
                            payload.path("source").asText(),
                            payload.path("sender").path("name").asText(""),
                            payload.path("payload").path("text").asText(""));
                }
            }
        } catch (Exception ex) {
            log.warn("[WHATSAPP] callback could not be handled: {}", ex.toString());
        }
        return ResponseEntity.ok().build();
    }
}
