package com.commercesphere.cart.dto;
import jakarta.validation.constraints.NotBlank;
public record MergeCartRequest(@NotBlank String guestId, @NotBlank String userId) {}
