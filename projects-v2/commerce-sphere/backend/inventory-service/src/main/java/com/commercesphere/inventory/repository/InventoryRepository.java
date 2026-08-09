package com.commercesphere.inventory.repository;
import com.commercesphere.inventory.document.Inventory;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;import java.util.Optional;
public interface InventoryRepository extends MongoRepository<Inventory, String> {
    List<Inventory> findByProductId(String productId);
    Optional<Inventory> findByProductIdAndWarehouseId(String productId, String warehouseId);
    List<Inventory> findByAvailableQuantityLessThan(int threshold);
}
