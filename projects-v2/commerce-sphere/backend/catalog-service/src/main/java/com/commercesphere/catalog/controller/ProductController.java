package com.commercesphere.catalog.controller;

import com.commercesphere.catalog.dto.*;
import com.commercesphere.catalog.service.ImageService;
import com.commercesphere.catalog.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/catalog/products")
@Tag(name = "Products", description = "Product management endpoints")
public class ProductController {

    private final ProductService productService;
    private final ImageService imageService;

    public ProductController(ProductService productService, ImageService imageService) {
        this.productService = productService;
        this.imageService = imageService;
    }

    @GetMapping
    @Operation(summary = "Search and list products with pagination and filters")
    public ResponseEntity<PagedResponse<ProductDto>> search(ProductSearchRequest request) {
        return ResponseEntity.ok(productService.search(request));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured products")
    public ResponseEntity<PagedResponse<ProductDto>> getFeatured(ProductSearchRequest request) {
        return ResponseEntity.ok(productService.getFeatured(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ProductDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new product (admin)")
    public ResponseEntity<ProductDto> create(@Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.ok(productService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a product (admin)")
    public ResponseEntity<ProductDto> update(@PathVariable String id, @Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product (admin)")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/images")
    @Operation(summary = "Upload product image")
    public ResponseEntity<String> uploadImage(@PathVariable String id, @RequestParam("file") MultipartFile file) {
        String url = imageService.upload(file);
        return ResponseEntity.ok(url);
    }
}
