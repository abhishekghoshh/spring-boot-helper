package com.commercesphere.catalog.controller;

import com.commercesphere.catalog.dto.BrandDto;
import com.commercesphere.catalog.service.BrandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog/brands")
@Tag(name = "Brands", description = "Brand management endpoints")
public class BrandController {

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @GetMapping
    @Operation(summary = "List all brands")
    public ResponseEntity<List<BrandDto>> getAll() {
        return ResponseEntity.ok(brandService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get brand by ID")
    public ResponseEntity<BrandDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(brandService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create a brand (admin)")
    public ResponseEntity<BrandDto> create(@Valid @RequestBody BrandDto dto) {
        return ResponseEntity.ok(brandService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a brand (admin)")
    public ResponseEntity<BrandDto> update(@PathVariable String id, @Valid @RequestBody BrandDto dto) {
        return ResponseEntity.ok(brandService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a brand (admin)")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        brandService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
