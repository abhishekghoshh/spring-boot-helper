package com.commercemesh.order.dto;

import jakarta.validation.constraints.NotBlank;

public record ShippingAddress(
        @NotBlank(message = "Address line 1 is required")
        String addressLine1,

        String addressLine2,

        @NotBlank(message = "City is required")
        String city,

        @NotBlank(message = "State is required")
        String state,

        @NotBlank(message = "Country is required")
        String country,

        @NotBlank(message = "Zip code is required")
        String zipCode,

        String phone
) {}
