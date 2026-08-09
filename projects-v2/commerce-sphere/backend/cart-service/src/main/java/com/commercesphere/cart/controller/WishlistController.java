package com.commercesphere.cart.controller;
import com.commercesphere.cart.dto.WishlistItemDto;
import com.commercesphere.cart.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/cart/wishlist")
@Tag(name = "Wishlist", description = "Wishlist endpoints")
public class WishlistController {
    private final WishlistService wishlistService;
    public WishlistController(WishlistService wishlistService) { this.wishlistService = wishlistService; }

    @GetMapping("/{userId}")
    @Operation(summary = "Get wishlist")
    public ResponseEntity<List<WishlistItemDto>> get(@PathVariable String userId) { return ResponseEntity.ok(wishlistService.getWishlist(userId)); }

    @PostMapping("/{userId}/items")
    @Operation(summary = "Add item to wishlist")
    public ResponseEntity<Void> add(@PathVariable String userId, @Valid @RequestBody WishlistItemDto item) { wishlistService.addItem(userId, item); return ResponseEntity.ok().build(); }

    @DeleteMapping("/{userId}/items/{productId}")
    @Operation(summary = "Remove from wishlist")
    public ResponseEntity<Void> remove(@PathVariable String userId, @PathVariable String productId) { wishlistService.removeItem(userId, productId); return ResponseEntity.noContent().build(); }

    @GetMapping("/{userId}/check/{productId}")
    @Operation(summary = "Check if in wishlist")
    public ResponseEntity<Boolean> check(@PathVariable String userId, @PathVariable String productId) { return ResponseEntity.ok(wishlistService.isInWishlist(userId, productId)); }
}
