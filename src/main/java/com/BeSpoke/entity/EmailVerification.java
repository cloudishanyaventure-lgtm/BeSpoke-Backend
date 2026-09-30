package com.BeSpoke.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A one-time code sent to an address that has no account yet. Signing up starts with the
 * code, not with a form: the visitor proves the address is theirs first, and only then are
 * they asked who they are — so a typo or an abandoned form never leaves a half-made User
 * behind, which is exactly what the old order did.
 *
 * <p>The row lives for ten minutes and dies when the account is created. Codes for an
 * address that <em>does</em> have an account never come here — those still live on
 * {@link User}, because that flow is a sign-in, not a signup.
 */
@Entity
@Table(name = "email_verifications")
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(nullable = false)
    private Instant expiresAt;

    /** Burned after five wrong guesses, same as a sign-in code. */
    @Column(nullable = false)
    private int attempts;

    public EmailVerification() {
    }

    public EmailVerification(String email, String code, Instant expiresAt) {
        this.email = email;
        this.code = code;
        this.expiresAt = expiresAt;
    }

    public boolean isLive() {
        return expiresAt != null && expiresAt.isAfter(Instant.now());
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }
}
