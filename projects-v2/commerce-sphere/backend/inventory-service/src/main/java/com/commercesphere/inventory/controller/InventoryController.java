package com.commercesphere.inventory.controller;
import com.commercesphere.inventory.dto.*;
import com.commercesphere.inventory.service.*;
import io.swagger.v3.oas.annotations.Operation;import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/inventory")
@Tag(name = "Inventory", description = "Inventory management endpoints")
public class InventoryController {
    private final InventoryService invSvc; private final ReservationService resSvc;
    public InventoryController(InventoryService invSvc, ReservationService resSvc) { this.invSvc = invSvc; this.resSvc = resSvc; }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get inventory for a product")
    public ResponseEntity<List<InventoryDto>> getByProduct(@PathVariable String productId) { return ResponseEntity.ok(invSvc.getByProduct(productId)); }

    @PostMapping("/stock")
    @Operation(summary = "Add stock")
    public ResponseEntity<InventoryDto> addStock(@RequestParam String productId, @RequestParam String warehouseId, @RequestParam String productName, @RequestParam int quantity) { return ResponseEntity.ok(invSvc.addStock(productId, warehouseId, productName, quantity)); }

    @PostMapping("/check")
    @Operation(summary = "Check stock availability")
    public ResponseEntity<Boolean> check(@Valid @RequestBody StockCheckRequest req) { return ResponseEntity.ok(invSvc.checkAvailability(req.productId(), req.quantity())); }

    @PostMapping("/reserve")
    @Operation(summary = "Reserve inventory")
    public ResponseEntity<InventoryDto> reserve(@Valid @RequestBody ReservationRequest req) {
        InventoryDto result = invSvc.reserve(req.productId(), req.orderId(), req.quantity());
        return ResponseEntity.ok(result);
    }
}
