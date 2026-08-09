package com.commercesphere.inventory.controller;
import com.commercesphere.inventory.dto.WarehouseDto;
import com.commercesphere.inventory.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/inventory/warehouses")
@Tag(name = "Warehouses", description = "Warehouse management")
public class WarehouseController {
    private final WarehouseService svc;
    public WarehouseController(WarehouseService svc) { this.svc = svc; }
    @GetMapping public ResponseEntity<List<WarehouseDto>> getAll() { return ResponseEntity.ok(svc.getAll()); }
    @PostMapping public ResponseEntity<WarehouseDto> create(@Valid @RequestBody WarehouseDto dto) { return ResponseEntity.ok(svc.create(dto)); }
}
