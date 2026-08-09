package com.loansphere.customer.controller;

import com.loansphere.common.api.ApiResponse;
import com.loansphere.common.security.CurrentUser;
import com.loansphere.customer.model.CustomerProfile;
import com.loansphere.customer.service.CustomerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerProfileController {

    private final CustomerProfileService profileService;

    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<CustomerProfile>> createProfile(
            @CurrentUser String userId, @Valid @RequestBody CustomerProfile profile) {
        profile.setUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(profileService.create(profile)));
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SUPER_ADMIN', 'BANK_ADMIN')")
    public ResponseEntity<ApiResponse<CustomerProfile>> getProfile(@CurrentUser String userId) {
        return ResponseEntity.ok(ApiResponse.success(profileService.getByUserId(userId)));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<CustomerProfile>> updateProfile(
            @CurrentUser String userId, @Valid @RequestBody CustomerProfile profile) {
        return ResponseEntity.ok(ApiResponse.success(profileService.update(userId, profile)));
    }
}
