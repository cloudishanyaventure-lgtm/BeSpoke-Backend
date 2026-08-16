package com.BeSpoke.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** {@code partner} = the request came from the partner sign-in, not the homeowner one. */
public record OtpVerifyRequest(@NotBlank @Email String email, @NotBlank String code,
                               boolean partner) {
}
