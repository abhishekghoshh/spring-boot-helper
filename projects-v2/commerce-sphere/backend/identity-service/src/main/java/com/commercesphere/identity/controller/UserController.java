package com.commercesphere.identity.controller;

import com.commercesphere.identity.dto.ChangePasswordRequest;
import com.commercesphere.identity.dto.UserProfileDto;
import com.commercesphere.identity.service.JwtService;
import com.commercesphere.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User profile management")
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    public UserController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @GetMapping("/profile")
    @Operation(summary = "Get current user profile", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserProfileDto> getProfile(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String userId = jwtService.getUserIdFromToken(token);
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    @PutMapping("/password")
    @Operation(summary = "Change password", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> changePassword(@RequestHeader("Authorization") String authHeader,
                                                @Valid @RequestBody ChangePasswordRequest request) {
        String token = authHeader.substring(7);
        String userId = jwtService.getUserIdFromToken(token);
        userService.changePassword(userId, request);
        return ResponseEntity.ok().build();
    }
}
