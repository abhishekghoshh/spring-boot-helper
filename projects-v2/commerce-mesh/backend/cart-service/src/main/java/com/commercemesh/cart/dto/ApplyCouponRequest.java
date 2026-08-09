package com.commercemesh.cart.dto;

import jakarta.validation.constraints.NotBlank;

public record ApplyCouponRequest(
        @NotBlank String couponCode
) {}
