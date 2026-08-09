package com.commercemesh.user.dto;

import com.commercemesh.user.entity.Address;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank(message = "Address line 1 is required")
        @Size(max = 255, message = "Address line 1 must not exceed 255 characters")
        String addressLine1,

        @Size(max = 255, message = "Address line 2 must not exceed 255 characters")
        String addressLine2,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @NotBlank(message = "State is required")
        @Size(max = 100, message = "State must not exceed 100 characters")
        String state,

        @NotBlank(message = "Country is required")
        @Size(max = 100, message = "Country must not exceed 100 characters")
        String country,

        @NotBlank(message = "Zip code is required")
        @Size(max = 20, message = "Zip code must not exceed 20 characters")
        String zipCode,

        boolean isDefault,

        @NotBlank(message = "Address type is required")
        String addressType
) {
        public Address.AddressType toAddressType() {
                return Address.AddressType.valueOf(addressType.toUpperCase());
        }
}
