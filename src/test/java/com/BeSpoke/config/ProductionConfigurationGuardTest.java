package com.BeSpoke.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class ProductionConfigurationGuardTest {
    MockEnvironment settings() {
        return new MockEnvironment().withProperty("app.jwt.secret", "test-only-unique-secret-with-over-32-bytes")
                .withProperty("app.crypto.key", java.util.Base64.getEncoder().encodeToString(new byte[32]))
                .withProperty("app.seed.demo-enabled", "false")
                .withProperty("app.storage.bucket", "test-assets")
                .withProperty("spring.datasource.password", "test-only")
                .withProperty("app.cors.allowed-origins", "https://example.com,https://crm.example.com");
    }
    @Test void acceptsExplicitProductionConfiguration() {
        assertDoesNotThrow(() -> new ProductionConfigurationGuard(settings()));
    }
    @Test void rejectsDevelopmentKeysAndDemoAccounts() {
        for (String[] setting : new String[][] {
                {"app.jwt.secret", "n5SmGiI.-!dx/Ek5FR][ib&r5t-X&FoeX1Z*%ud%Kim"},
                {"app.crypto.key", "8Zq2mJ0xR7vT1cN5bK9wY3sD6fH4gL8pA2eU7iO0rQ4="},
                {"app.crypto.key", "invalid-base64"}, {"app.seed.demo-enabled", "true"},
                {"app.storage.bucket", ""}, {"spring.datasource.password", ""},
                {"app.cors.allowed-origins", "http://localhost:*"},
                {"app.cors.allowed-origins", "https://example.com/path"}}) {
            assertThrows(IllegalStateException.class, () -> new ProductionConfigurationGuard(settings().withProperty(setting[0], setting[1])));
        }
    }
}
