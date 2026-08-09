package com.commercemesh.inventory.controller;

import com.commercemesh.inventory.dto.InventoryRequest;
import com.commercemesh.inventory.dto.InventoryResponse;
import com.commercemesh.inventory.dto.StockCheckRequest;
import com.commercemesh.inventory.dto.StockCheckResponse;
import com.commercemesh.inventory.dto.StockReservationRequest;
import com.commercemesh.inventory.dto.StockReservationResponse;
import com.commercemesh.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/check")
    public ResponseEntity<StockCheckResponse> checkStock(@Valid @ModelAttribute StockCheckRequest request) {
        StockCheckResponse response = inventoryService.checkStockDetailed(
                request.productId(), request.quantity());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<List<InventoryResponse>> getInventory(@PathVariable Long productId) {
        List<InventoryResponse> inventory = inventoryService.getInventory(productId);
        return ResponseEntity.ok(inventory);
    }

    @GetMapping("/{productId}/warehouses/{warehouseId}")
    public ResponseEntity<InventoryResponse> getInventoryByWarehouse(
            @PathVariable Long productId,
            @PathVariable Long warehouseId) {
        InventoryResponse inventory = inventoryService.getInventoryByWarehouse(productId, warehouseId);
        return ResponseEntity.ok(inventory);
    }

    @PostMapping
    @PreAuthorize("hasRole('INVENTORY_MANAGER')")
    public ResponseEntity<InventoryResponse> createInventory(@Valid @RequestBody InventoryRequest request) {
        InventoryResponse inventory = inventoryService.createInventory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(inventory);
    }

    @PutMapping("/{productId}/warehouses/{warehouseId}")
    @PreAuthorize("hasRole('INVENTORY_MANAGER')")
    public ResponseEntity<InventoryResponse> updateInventory(
            @PathVariable Long productId,
            @PathVariable Long warehouseId,
            @Valid @RequestBody InventoryRequest request) {
        InventoryResponse inventory = inventoryService.updateInventory(productId, warehouseId, request.quantity());
        return ResponseEntity.ok(inventory);
    }

    @PostMapping("/reserve")
    @PreAuthorize("hasRole('INVENTORY_MANAGER')")
    public ResponseEntity<StockReservationResponse> reserveStock(
            @Valid @RequestBody StockReservationRequest request) {
        StockReservationResponse reservation = inventoryService.reserveStock(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
    }

    @PostMapping("/reservations/{orderId}/confirm")
    @PreAuthorize("hasRole('INVENTORY_MANAGER')")
    public ResponseEntity<StockReservationResponse> confirmReservation(@PathVariable Long orderId) {
        StockReservationResponse reservation = inventoryService.confirmReservation(orderId);
        return ResponseEntity.ok(reservation);
    }

    @PostMapping("/reservations/{orderId}/cancel")
    @PreAuthorize("hasRole('INVENTORY_MANAGER')")
    public ResponseEntity<StockReservationResponse> cancelReservation(@PathVariable Long orderId) {
        StockReservationResponse reservation = inventoryService.cancelReservation(orderId);
        return ResponseEntity.ok(reservation);
    }
}
