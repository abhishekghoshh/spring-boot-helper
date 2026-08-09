package com.commercemesh.user.controller;

import com.commercemesh.user.dto.AddressRequest;
import com.commercemesh.user.dto.AddressResponse;
import com.commercemesh.user.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/{userId}/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<List<AddressResponse>> getUserAddresses(@PathVariable Long userId) {
        return ResponseEntity.ok(addressService.getUserAddresses(userId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<AddressResponse> addAddress(@PathVariable Long userId, @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(addressService.addAddress(userId, request));
    }

    @PutMapping("/{addressId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.updateAddress(userId, addressId, request));
    }

    @DeleteMapping("/{addressId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long userId, @PathVariable Long addressId) {
        addressService.deleteAddress(userId, addressId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{addressId}/default")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<AddressResponse> setDefaultAddress(@PathVariable Long userId, @PathVariable Long addressId) {
        return ResponseEntity.ok(addressService.setDefaultAddress(userId, addressId));
    }
}
