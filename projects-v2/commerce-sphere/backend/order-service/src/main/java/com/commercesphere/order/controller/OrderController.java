package com.commercesphere.order.controller;
import com.commercesphere.order.dto.OrderDto;
import com.commercesphere.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {
    private final OrderService orderService;
    public OrderController(OrderService orderService) { this.orderService = orderService; }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<OrderDto> get(@PathVariable String orderId) { return ResponseEntity.ok(orderService.getOrder(orderId)); }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user's orders")
    public ResponseEntity<List<OrderDto>> getUserOrders(@PathVariable String userId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return ResponseEntity.ok(orderService.getUserOrders(userId, page, size)); }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<OrderDto> cancel(@PathVariable String orderId) { return ResponseEntity.ok(orderService.cancelOrder(orderId)); }
}
