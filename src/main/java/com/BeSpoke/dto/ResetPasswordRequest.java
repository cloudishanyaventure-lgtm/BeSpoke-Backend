package com.BeSpoke.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Email String email,
        @NotBlank String code,
        @NotBlank @Size(min = 8, message = "Use at least 8 characters") String password
) {
}
