package com.commercemesh.inventory.repository;

import com.commercemesh.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    List<Inventory> findByProductId(Long productId);

    Optional<Inventory> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId AND i.quantity - i.reservedQuantity >= :quantity")
    List<Inventory> findAvailableByProductIdAndQuantity(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Query("SELECT COALESCE(SUM(i.quantity - i.reservedQuantity), 0) FROM Inventory i WHERE i.productId = :productId")
    int getTotalAvailableQuantityByProductId(@Param("productId") Long productId);
}
