package com.commercesphere.order.controller;
import com.commercesphere.order.dto.CheckoutRequest;
import com.commercesphere.order.dto.OrderDto;
import com.commercesphere.order.service.CheckoutService;
import io.swagger.v3.oas.annotations.Operation;import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/orders/checkout")
@Tag(name = "Checkout", description = "Checkout endpoint")
public class CheckoutController {
    private final CheckoutService checkoutService;
    public CheckoutController(CheckoutService checkoutService) { this.checkoutService = checkoutService; }

    @PostMapping
    @Operation(summary = "Checkout and place order")
    public ResponseEntity<OrderDto> checkout(@Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(checkoutService.checkout(request));
    }
}
