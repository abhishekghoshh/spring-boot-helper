package com.commercemesh.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "inventory-service")
public interface InventoryClient {

    @GetMapping("/api/v1/inventory/{productId}/stock")
    Map<String, Object> checkStock(@PathVariable Long productId);

    @PostMapping("/api/v1/inventory/reserve")
    Map<String, Object> reserveStock(@RequestBody StockReservationRequest request);

    record StockReservationRequest(Long productId, int quantity, String referenceId) {}
}
