package com.commercemesh.inventory.config;

import com.commercemesh.inventory.entity.Inventory;
import com.commercemesh.inventory.entity.Warehouse;
import com.commercemesh.inventory.repository.InventoryRepository;
import com.commercemesh.inventory.repository.WarehouseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;

@Configuration
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;

    public DataInitializer(WarehouseRepository warehouseRepository,
                           InventoryRepository inventoryRepository) {
        this.warehouseRepository = warehouseRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public void run(String... args) {
        if (warehouseRepository.count() > 0) {
            log.info("Data already initialized, skipping seed.");
            return;
        }

        log.info("Seeding initial data...");

        // --- 2 sample warehouses ---
        Warehouse warehouse1 = new Warehouse("Main Warehouse", "123 Industrial Blvd, Chicago, IL 60601", true);
        Warehouse warehouse2 = new Warehouse("East Coast Distribution", "456 Commerce Dr, Newark, NJ 07101", true);

        warehouse1 = warehouseRepository.save(warehouse1);
        warehouse2 = warehouseRepository.save(warehouse2);

        // --- 5 inventory records ---
        Inventory inv1 = new Inventory(1L, warehouse1.getId(), 500, 50, 200);
        Inventory inv2 = new Inventory(2L, warehouse1.getId(), 300, 30, 100);
        Inventory inv3 = new Inventory(3L, warehouse1.getId(), 150, 20, 80);
        Inventory inv4 = new Inventory(1L, warehouse2.getId(), 250, 40, 150);
        Inventory inv5 = new Inventory(2L, warehouse2.getId(), 100, 25, 75);

        inventoryRepository.saveAll(List.of(inv1, inv2, inv3, inv4, inv5));

        log.info("Seeded 2 warehouses and 5 inventory records.");
    }
}
