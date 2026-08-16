package com.BeSpoke.security;

import com.BeSpoke.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Verifies a Google Identity Services ID token by asking Google — signature, expiry
 * and issuer are all checked on their side, so there is no crypto (or extra
 * dependency) here. We still check the audience ourselves: a token minted for some
 * other site's client id must not sign anyone in here.
 */
@Component
public class GoogleTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifier.class);
    private static final String TOKENINFO = "https://oauth2.googleapis.com/tokeninfo?id_token=";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json = new ObjectMapper();
    private final String clientId;

    public GoogleTokenVerifier(@Value("${app.google.client-id:}") String clientId) {
        this.clientId = clientId == null ? "" : clientId.trim();
    }

    public boolean isConfigured() {
        return !clientId.isBlank();
    }

    /** @return the verified, Google-confirmed email address, lower-cased. */
    public String verifiedEmail(String idToken) {
        if (!isConfigured()) {
            throw new BadRequestException("Google sign-in isn't configured on this server");
        }
        JsonNode claims;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TOKENINFO
                            + URLEncoder.encode(idToken, StandardCharsets.UTF_8)))
                    .timeout(Duration.ofSeconds(8))
                    .GET().build();
            HttpResponse<String> response =
                    http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new BadRequestException("Google sign-in failed — please try again");
            }
            claims = json.readTree(response.body());
        } catch (BadRequestException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("Couldn't reach Google to verify the sign-in");
        } catch (Exception ex) {
            log.warn("[GOOGLE] tokeninfo call failed: {}", ex.getMessage());
            throw new BadRequestException("Couldn't reach Google to verify the sign-in");
        }
        if (!clientId.equals(claims.path("aud").asText())) {
            throw new BadRequestException("Google sign-in failed — please try again");
        }
        if (!claims.path("email_verified").asBoolean(false)) {
            throw new BadRequestException("That Google account has an unverified email");
        }
        String email = claims.path("email").asText("");
        if (email.isBlank()) {
            throw new BadRequestException("Google didn't share an email for that account");
        }
        return email.toLowerCase();
    }
}
