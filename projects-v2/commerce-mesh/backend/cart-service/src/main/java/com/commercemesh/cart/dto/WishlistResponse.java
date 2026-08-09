package com.commercemesh.cart.dto;

import java.util.List;

public record WishlistResponse(
        String id,
        String userId,
        List<WishlistItemResponse> items
) {}
