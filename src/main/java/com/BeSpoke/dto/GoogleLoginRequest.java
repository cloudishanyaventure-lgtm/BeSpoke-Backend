package com.BeSpoke.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * {@code credential} is the ID token Google Identity Services hands the browser;
 * {@code partner} says which sign-in page it came from, so each side stays separate.
 */
public record GoogleLoginRequest(@NotBlank String credential, boolean partner) {
}
