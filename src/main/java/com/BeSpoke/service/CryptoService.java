package com.BeSpoke.service;

import com.BeSpoke.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption at rest for message bodies. Ciphertext is stored as
 * {@code gcm:<base64 iv>:<base64 ciphertext>}; anything without the prefix is
 * returned verbatim, so plaintext rows written before this change still read back.
 *
 * <p>At rest, not end-to-end: the server holds the key so admins can still moderate
 * and recover threads. See docs/CHANGES_V3.md §8b.
 */
@Service
public class CryptoService {

    private static final String PREFIX = "gcm:";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecureRandom random = new SecureRandom();
    private final SecretKey key;

    public CryptoService(@Value("${app.crypto.key}") String base64Key) {
        byte[] raw = Base64.getDecoder().decode(base64Key.trim());
        if (raw.length != 32) {
            throw new IllegalStateException(
                    "app.crypto.key must be 32 bytes base64-encoded for AES-256, got " + raw.length);
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    /** Null-safe. Returns {@code gcm:<iv>:<ciphertext>}. */
    public String encrypt(String plain) {
        if (plain == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            Base64.Encoder b64 = Base64.getEncoder();
            return PREFIX + b64.encodeToString(iv) + ":" + b64.encodeToString(cipherText);
        } catch (Exception e) {
            throw new IllegalStateException("Could not encrypt message body", e);
        }
    }

    /** Returns the input unchanged when it is not {@code gcm:}-prefixed (legacy plaintext). */
    public String decrypt(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) {
            return stored;
        }
        // base64 never contains ':', so a plain split is enough.
        String[] parts = stored.split(":", 3);
        if (parts.length != 3) {
            throw new BadRequestException("Stored message is malformed");
        }
        try {
            Base64.Decoder b64 = Base64.getDecoder();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, b64.decode(parts[1])));
            return new String(cipher.doFinal(b64.decode(parts[2])), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Could not decrypt message body", e);
        }
    }
}
