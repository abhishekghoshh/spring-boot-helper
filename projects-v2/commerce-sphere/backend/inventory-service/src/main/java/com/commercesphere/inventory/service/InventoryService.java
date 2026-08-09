package com.commercesphere.inventory.service;
import com.commercesphere.inventory.document.*;
import com.commercesphere.inventory.dto.*;
import com.commercesphere.inventory.repository.*;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class InventoryService {
    private final InventoryRepository invRepo; private final WarehouseRepository whRepo;
    public InventoryService(InventoryRepository invRepo, WarehouseRepository whRepo) { this.invRepo = invRepo; this.whRepo = whRepo; }

    public List<InventoryDto> getByProduct(String productId) {
        return invRepo.findByProductId(productId).stream().map(this::toDto).toList();
    }
    public InventoryDto getByProductAndWarehouse(String productId, String warehouseId) {
        return invRepo.findByProductIdAndWarehouseId(productId, warehouseId).map(this::toDto).orElse(null);
    }
    public InventoryDto addStock(String productId, String warehouseId, String productName, int quantity) {
        Warehouse wh = whRepo.findById(warehouseId).orElseThrow(() -> new RuntimeException("Warehouse not found"));
        var inv = invRepo.findByProductIdAndWarehouseId(productId, warehouseId).orElse(null);
        if (inv == null) {
            inv = Inventory.builder().productId(productId).productName(productName).warehouseId(warehouseId)
                    .warehouseName(wh.getName()).quantity(quantity).reservedQuantity(0).build();
        } else { inv.setQuantity(inv.getQuantity() + quantity); }
        return toDto(invRepo.save(inv));
    }
    public boolean checkAvailability(String productId, int quantity) {
        return invRepo.findByProductId(productId).stream().mapToInt(Inventory::getAvailableQuantity).sum() >= quantity;
    }
    public InventoryDto reserve(String productId, String orderId, int quantity) {
        var inventories = invRepo.findByProductId(productId);
        int remaining = quantity;
        for (var inv : inventories) {
            int available = inv.getAvailableQuantity();
            if (available > 0) {
                int toReserve = Math.min(available, remaining);
                inv.setReservedQuantity(inv.getReservedQuantity() + toReserve);
                invRepo.save(inv);
                remaining -= toReserve;
                if (remaining == 0) break;
            }
        }
        if (remaining > 0) throw new RuntimeException("Insufficient stock");
        return toDto(invRepo.findByProductId(productId).stream()
                .reduce((a, b) -> Inventory.builder().productId(a.getProductId()).quantity(a.getQuantity() + b.getQuantity())
                        .reservedQuantity(a.getReservedQuantity() + b.getReservedQuantity()).build()).orElseThrow());
    }
    public void releaseReservation(String productId, String orderId, int quantity) {
        var inventories = invRepo.findByProductId(productId);
        int remaining = quantity;
        for (var inv : inventories) {
            if (inv.getReservedQuantity() > 0) {
                int toRelease = Math.min(inv.getReservedQuantity(), remaining);
                inv.setReservedQuantity(inv.getReservedQuantity() - toRelease);
                invRepo.save(inv);
                remaining -= toRelease;
                if (remaining == 0) break;
            }
        }
    }
    private InventoryDto toDto(Inventory i) { return new InventoryDto(i.getId(), i.getProductId(), i.getProductName(), i.getWarehouseId(), i.getWarehouseName(), i.getQuantity(), i.getReservedQuantity(), i.getAvailableQuantity()); }
}
