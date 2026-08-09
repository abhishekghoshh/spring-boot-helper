package com.loansphere.auth.service;

import com.loansphere.auth.model.User;
import com.loansphere.auth.repository.UserRepository;
import com.loansphere.common.exception.BadRequestException;
import com.loansphere.common.exception.ResourceNotFoundException;
import com.loansphere.common.exception.UnauthorizedException;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    public User register(String username, String email, String password, String firstName, String lastName) {
        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already registered");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRoles(Set.of("CUSTOMER"));
        user.setEmailVerified(false);
        user.setEnabled(true);
        user.setAccountNonLocked(true);

        return userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    public User findById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    public String generateAccessToken(User user) {
        try {
            JWSSigner signer = new MACSigner(jwtSecret.getBytes());
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(user.getId())
                    .claim("username", user.getUsername())
                    .claim("roles", new ArrayList<>(user.getRoles()))
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + jwtExpirationMs))
                    .jwtID(UUID.randomUUID().toString())
                    .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to generate JWT", e);
        }
    }

    public String generateRefreshToken(User user) {
        String refreshToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("refresh:" + refreshToken, user.getId(),
                Duration.ofMillis(refreshExpirationMs));
        return refreshToken;
    }

    public String generatePasswordResetToken(User user) {
        String resetToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("pwd-reset:" + resetToken, user.getId(),
                Duration.ofMinutes(15));
        return resetToken;
    }

    public String generateEmailVerificationToken(User user) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("email-verify:" + token, user.getId(),
                Duration.ofHours(24));
        return token;
    }

    public void blacklistToken(String accessToken, long expirationMs) {
        redisTemplate.opsForValue().set("blacklist:" + accessToken, "revoked",
                Duration.ofMillis(expirationMs));
    }

    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token));
    }
}
