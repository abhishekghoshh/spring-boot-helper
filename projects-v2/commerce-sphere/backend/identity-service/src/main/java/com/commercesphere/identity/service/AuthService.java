package com.commercesphere.identity.service;

import com.commercesphere.identity.dto.*;
import com.commercesphere.identity.entity.User;
import com.commercesphere.identity.exception.InvalidCredentialsException;
import com.commercesphere.identity.exception.TokenRefreshException;
import com.commercesphere.identity.exception.UserAlreadyExistsException;
import com.commercesphere.identity.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException("Username '" + request.username() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Email '" + request.email() + "' is already registered");
        }

        Instant now = Instant.now();
        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .enabled(true)
                .roles(Set.of("ROLE_USER"))
                .createdAt(now)
                .updatedAt(now)
                .build();

        userRepository.save(user);
        return buildTokenResponse(user);
    }

    public TokenResponse login(LoginRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.username());
        if (userOpt.isEmpty()) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        return buildTokenResponse(user);
    }

    public TokenResponse refreshToken(String refreshTokenId, String jwtRefreshToken) {
        if (!refreshTokenService.validateRefreshToken(refreshTokenId, jwtRefreshToken)) {
            throw new TokenRefreshException("Invalid or expired refresh token");
        }

        String userId = refreshTokenService.getUserIdFromTokenId(refreshTokenId);
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new TokenRefreshException("User not found for refresh token");
        }

        refreshTokenService.revokeRefreshToken(refreshTokenId);
        return buildTokenResponse(userOpt.get());
    }

    private TokenResponse buildTokenResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String jwtRefreshToken = jwtService.generateRefreshToken(user);
        String refreshTokenId = refreshTokenService.createRefreshToken(user.getId(), jwtRefreshToken);

        return new TokenResponse(
                accessToken,
                refreshTokenId + "." + jwtRefreshToken,
                "Bearer",
                jwtService.getExpirationMs(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName() + " " + user.getLastName()
        );
    }
}
