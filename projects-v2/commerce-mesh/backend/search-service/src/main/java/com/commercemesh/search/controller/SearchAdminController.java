package com.commercemesh.search.controller;

import com.commercemesh.search.document.ProductDocument;
import com.commercemesh.search.dto.ProductDocumentResponse;
import com.commercemesh.search.service.SearchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/search/admin")
public class SearchAdminController {

    private final SearchService searchService;

    public SearchAdminController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * POST /api/v1/search/admin/index — index a single product (ADMIN only).
     */
    @PostMapping("/index")
    public ResponseEntity<ProductDocumentResponse> indexProduct(@Valid @RequestBody ProductDocument product) {
        ProductDocument saved = searchService.indexProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    /**
     * DELETE /api/v1/search/admin/index/{productId} — remove product from index (ADMIN only).
     */
    @DeleteMapping("/index/{productId}")
    public ResponseEntity<Void> removeProduct(@PathVariable String productId) {
        searchService.removeProductFromIndex(productId);
        return ResponseEntity.noContent().build();
    }

    private ProductDocumentResponse toResponse(ProductDocument doc) {
        return new ProductDocumentResponse(
                doc.getId(),
                doc.getName(),
                doc.getDescription(),
                doc.getBrand(),
                doc.getCategory(),
                doc.getPrice(),
                doc.getImage(),
                doc.getAverageRating(),
                doc.getReviewCount(),
                doc.getTags(),
                doc.isActive(),
                doc.getCreatedAt(),
                doc.getTextScore()
        );
    }
}
