package com.commercesphere.cart.controller;
import com.commercesphere.cart.dto.*;
import com.commercesphere.cart.service.CartService;
import io.swagger.v3.oas.annotations.Operation;import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/cart")
@Tag(name = "Cart", description = "Shopping cart endpoints")
public class CartController {
    private final CartService cartService;
    public CartController(CartService cartService) { this.cartService = cartService; }

    @GetMapping("/{cartId}")
    @Operation(summary = "Get cart by ID")
    public ResponseEntity<CartDto> getCart(@PathVariable String cartId) { return ResponseEntity.ok(cartService.getCart(cartId)); }

    @PostMapping("/{cartId}/items")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<CartDto> addItem(@PathVariable String cartId, @Valid @RequestBody CartItemDto item) { return ResponseEntity.ok(cartService.addItem(cartId, item)); }

    @DeleteMapping("/{cartId}/items/{productId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<CartDto> removeItem(@PathVariable String cartId, @PathVariable String productId) { return ResponseEntity.ok(cartService.removeItem(cartId, productId)); }

    @PutMapping("/{cartId}/items/{productId}")
    @Operation(summary = "Update item quantity")
    public ResponseEntity<CartDto> updateQty(@PathVariable String cartId, @PathVariable String productId, @RequestParam int quantity) { return ResponseEntity.ok(cartService.updateQuantity(cartId, productId, quantity)); }

    @DeleteMapping("/{cartId}")
    @Operation(summary = "Clear cart")
    public ResponseEntity<Void> clearCart(@PathVariable String cartId) { cartService.clearCart(cartId); return ResponseEntity.noContent().build(); }

    @PostMapping("/merge")
    @Operation(summary = "Merge guest cart into user cart")
    public ResponseEntity<CartDto> merge(@Valid @RequestBody MergeCartRequest req) { return ResponseEntity.ok(cartService.mergeGuestCart(req.guestId(), req.userId())); }
}
