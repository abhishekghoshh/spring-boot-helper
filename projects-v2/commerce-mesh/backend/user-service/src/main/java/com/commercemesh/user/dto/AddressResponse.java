package com.commercemesh.user.dto;

import com.commercemesh.user.entity.Address;

import java.time.Instant;

public record AddressResponse(
        Long id,
        Long userId,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String country,
        String zipCode,
        boolean isDefault,
        String addressType,
        Instant createdAt,
        Instant updatedAt
) {
        public static AddressResponse fromEntity(Address address) {
                return new AddressResponse(
                        address.getId(),
                        address.getUserId(),
                        address.getAddressLine1(),
                        address.getAddressLine2(),
                        address.getCity(),
                        address.getState(),
                        address.getCountry(),
                        address.getZipCode(),
                        address.isDefault(),
                        address.getAddressType().name(),
                        address.getCreatedAt(),
                        address.getUpdatedAt()
                );
        }
}
