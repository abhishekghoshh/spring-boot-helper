package com.commercesphere.inventory.repository;
import com.commercesphere.inventory.document.Warehouse;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface WarehouseRepository extends MongoRepository<Warehouse, String> {}
