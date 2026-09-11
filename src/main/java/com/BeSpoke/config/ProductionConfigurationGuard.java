package com.BeSpoke.config;

import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Refuse insecure production defaults before application runners create any accounts. */
@Component
@Profile("prod")
public class ProductionConfigurationGuard {
    public ProductionConfigurationGuard(Environment env) {
        String jwt = required(env, "app.jwt.secret");
        if (jwt.getBytes(StandardCharsets.UTF_8).length < 32 || jwt.equals("n5SmGiI.-!dx/Ek5FR][ib&r5t-X&FoeX1Z*%ud%Kim"))
            throw new IllegalStateException("JWT_SECRET must be a unique secret of at least 32 bytes");
        String crypto = required(env, "app.crypto.key");
        try {
            if (Base64.getDecoder().decode(crypto).length != 32 || crypto.equals("8Zq2mJ0xR7vT1cN5bK9wY3sD6fH4gL8pA2eU7iO0rQ4="))
                throw new IllegalArgumentException();
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("CRYPTO_KEY must be a unique base64-encoded 32-byte key");
        }
        if (env.getProperty("app.seed.demo-enabled", Boolean.class, true))
            throw new IllegalStateException("Demo account seeding must be disabled in production");
        // No bucket check: this VM keeps ~/uploads across deploys (only the jar is
        // replaced), so local-disk storage is a supported setup, not a misconfiguration.
        // FileStorageService already logs a loud [UPLOADS] warning when it is in use.
        required(env, "spring.datasource.password");
        String origins = required(env, "app.cors.allowed-origins");
        for (String origin : origins.split(",", -1)) {
            java.net.URI uri;
            try { uri = java.net.URI.create(origin.trim()); }
            catch (IllegalArgumentException ex) { throw new IllegalStateException("CORS_ALLOWED_ORIGINS must contain HTTPS origins"); }
            if (!"https".equals(uri.getScheme()) || uri.getHost() == null || origin.contains("*")
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                    || (uri.getPath() != null && !uri.getPath().isEmpty()))
                throw new IllegalStateException("CORS_ALLOWED_ORIGINS must contain exact HTTPS origins without paths or wildcards");
        }
    }

    private static String required(Environment env, String key) {
        String value = env.getProperty(key, "");
        if (value.isBlank()) throw new IllegalStateException("Missing production setting: " + key);
        return value;
    }
}
