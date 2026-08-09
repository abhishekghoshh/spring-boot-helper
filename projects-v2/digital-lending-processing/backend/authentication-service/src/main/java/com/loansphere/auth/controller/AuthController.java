package com.loansphere.auth.controller;

import com.loansphere.auth.dto.*;
import com.loansphere.auth.model.User;
import com.loansphere.auth.service.AuthService;
import com.loansphere.common.api.ApiResponse;
import com.loansphere.common.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(
                request.username(), request.email(), request.password(),
                request.firstName(), request.lastName());
        return ResponseEntity.ok(ApiResponse.success("Registration successful", toUserResponse(user)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        User user = authService.findByUsername(request.username());

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        String accessToken = authService.generateAccessToken(user);
        String refreshToken = authService.generateRefreshToken(user);

        return ResponseEntity.ok(ApiResponse.success(new TokenResponse(accessToken, refreshToken)));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        String userId = redisTemplate.opsForValue().get("refresh:" + request.refreshToken());
        if (userId == null) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        User user = authService.findById(userId);
        redisTemplate.delete("refresh:" + request.refreshToken());

        String accessToken = authService.generateAccessToken(user);
        String newRefreshToken = authService.generateRefreshToken(user);

        return ResponseEntity.ok(ApiResponse.success(new TokenResponse(accessToken, newRefreshToken)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String bearerToken) {
        String token = bearerToken.replace("Bearer ", "");
        Date expiration = new Date(System.currentTimeMillis() + 3600000);
        long remainingMs = expiration.getTime() - System.currentTimeMillis();
        authService.blacklistToken(token, remainingMs);

        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            User user = authService.findByUsername(request.email());
            String resetToken = authService.generatePasswordResetToken(user);
            // In production: send email with reset link
            return ResponseEntity.ok(ApiResponse.success("Password reset instructions sent to email", null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.success("If the account exists, password reset instructions have been sent", null));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        String userId = redisTemplate.opsForValue().get("pwd-reset:" + request.token());
        if (userId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid or expired reset token"));
        }

        User user = authService.findById(userId);
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        authService.findById(userId);
        redisTemplate.delete("pwd-reset:" + request.token());

        return ResponseEntity.ok(ApiResponse.success("Password reset successful", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@CurrentUser String userId) {
        User user = authService.findById(userId);
        return ResponseEntity.ok(ApiResponse.success(toUserResponse(user)));
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(), user.getUsername(), user.getEmail(),
                user.getFirstName(), user.getLastName(),
                user.getRoles(), user.isEmailVerified());
    }
}
