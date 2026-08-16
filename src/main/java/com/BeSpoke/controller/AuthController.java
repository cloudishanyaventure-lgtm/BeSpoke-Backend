package com.BeSpoke.controller;

import com.BeSpoke.dto.AuthResponse;
import com.BeSpoke.dto.ForgotPasswordRequest;
import com.BeSpoke.dto.GoogleLoginRequest;
import com.BeSpoke.dto.LoginRequest;
import com.BeSpoke.dto.OtpRequest;
import com.BeSpoke.dto.OtpVerifyRequest;
import com.BeSpoke.dto.RegisterRequest;
import com.BeSpoke.dto.ResetPasswordRequest;
import com.BeSpoke.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /** Always the same 200 — never reveals whether the email has an account. */
    @PostMapping("/otp/request")
    public ResponseEntity<Map<String, String>> requestOtp(@Valid @RequestBody OtpRequest request) {
        authService.requestOtp(request.email());
        return ResponseEntity.ok(Map.of("message", "If that email exists, we've sent a code."));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request.email(), request.code()));
    }

    /** Partner password reset — same generic 200 whether or not the account exists. */
    @PostMapping("/password/forgot")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.ok(Map.of("message", "If that email exists, we've sent a code."));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<AuthResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(
                request.email(), request.code(), request.password()));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> google(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(
                authService.loginWithGoogle(request.credential(), request.partner()));
    }
}
