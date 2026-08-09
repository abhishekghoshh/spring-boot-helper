package com.commercemesh.inventory.service;

import com.commercemesh.inventory.dto.WarehouseRequest;
import com.commercemesh.inventory.dto.WarehouseResponse;
import com.commercemesh.inventory.entity.Warehouse;
import com.commercemesh.inventory.exception.ResourceNotFoundException;
import com.commercemesh.inventory.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    public WarehouseService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    public List<WarehouseResponse> getAllWarehouses() {
        return warehouseRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public WarehouseResponse getWarehouseById(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", id));
        return toResponse(warehouse);
    }

    public WarehouseResponse createWarehouse(WarehouseRequest request) {
        Warehouse warehouse = new Warehouse(request.name(), request.location(), request.isActive());
        Warehouse saved = warehouseRepository.save(warehouse);
        return toResponse(saved);
    }

    public WarehouseResponse updateWarehouse(Long id, WarehouseRequest request) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", id));

        warehouse.setName(request.name());
        warehouse.setLocation(request.location());
        warehouse.setActive(request.isActive());

        Warehouse saved = warehouseRepository.save(warehouse);
        return toResponse(saved);
    }

    public void deleteWarehouse(Long id) {
        if (!warehouseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Warehouse", "id", id);
        }
        warehouseRepository.deleteById(id);
    }

    private WarehouseResponse toResponse(Warehouse w) {
        return new WarehouseResponse(
                w.getId(),
                w.getName(),
                w.getLocation(),
                w.isActive(),
                w.getCreatedAt(),
                w.getUpdatedAt()
        );
    }
}
