package com.BeSpoke.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Plain unit test — no Spring context, no database. */
class CryptoServiceTest {

    private final CryptoService crypto =
            new CryptoService("8Zq2mJ0xR7vT1cN5bK9wY3sD6fH4gL8pA2eU7iO0rQ4=");

    @Test
    void roundTripsIncludingUnicode() {
        String plain = "Kitchen drawing looks good — ✓ मराठी";
        String stored = crypto.encrypt(plain);
        assertTrue(stored.startsWith("gcm:"), stored);
        assertEquals(3, stored.split(":", 3).length);
        assertEquals(plain, crypto.decrypt(stored));
    }

    @Test
    void returnsLegacyPlaintextUnchanged() {
        assertEquals("written before encryption existed",
                crypto.decrypt("written before encryption existed"));
        assertNull(crypto.decrypt(null));
        assertNull(crypto.encrypt(null));
    }

    @Test
    void usesAFreshIvPerMessage() {
        assertNotEquals(crypto.encrypt("same text"), crypto.encrypt("same text"));
    }

    @Test
    void rejectsAKeyThatIsNot32Bytes() {
        assertThrows(IllegalStateException.class, () -> new CryptoService("c2hvcnQ="));
    }
}
