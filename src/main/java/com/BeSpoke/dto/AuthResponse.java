package com.BeSpoke.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Auth result. leadId is present only on registration (the lead created by signup).
 * newAccount is present only on /otp/confirm, and only when it is true: the code was
 * right but the address has no account, so the caller collects the rest and registers.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(String token, UserDto user, Long leadId, Boolean newAccount) {

    public AuthResponse(String token, UserDto user, Long leadId) {
        this(token, user, leadId, null);
    }

    /** The address is verified and free — nothing to sign in to yet. */
    public static AuthResponse unregistered() {
        return new AuthResponse(null, null, null, true);
    }
}
