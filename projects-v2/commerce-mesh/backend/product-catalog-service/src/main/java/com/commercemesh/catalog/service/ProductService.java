package com.commercemesh.catalog.service;

import com.commercemesh.catalog.document.Product;
import com.commercemesh.catalog.dto.PageResponse;
import com.commercemesh.catalog.dto.ProductRequest;
import com.commercemesh.catalog.dto.ProductResponse;
import com.commercemesh.catalog.exception.ResourceNotFoundException;
import com.commercemesh.catalog.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.findAll().stream().anyMatch(p -> p.getSku().equals(request.sku()))) {
            throw new IllegalArgumentException("Product with SKU '" + request.sku() + "' already exists");
        }

        Product product = new Product();
        product.setName(request.name());
        product.setDescription(request.description());
        product.setBrand(request.brand());
        product.setSku(request.sku());
        product.setPrice(request.price());
        product.setCompareAtPrice(request.compareAtPrice());
        product.setCurrency(request.currency() != null ? request.currency() : "USD");
        product.setCategoryId(request.categoryId());
        product.setImages(request.images() != null ? request.images() : List.of());
        product.setTags(request.tags() != null ? request.tags() : List.of());
        product.setAttributes(request.attributes() != null ? request.attributes() : java.util.Map.of());
        product.setActive(request.isActive());
        product.setFeatured(request.isFeatured());

        Product saved = productRepository.save(product);
        return ProductResponse.from(saved);
    }

    public ProductResponse updateProduct(String id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        product.setName(request.name());
        product.setDescription(request.description());
        product.setBrand(request.brand());
        product.setSku(request.sku());
        product.setPrice(request.price());
        product.setCompareAtPrice(request.compareAtPrice());
        product.setCurrency(request.currency() != null ? request.currency() : "USD");
        product.setCategoryId(request.categoryId());
        product.setImages(request.images() != null ? request.images() : List.of());
        product.setTags(request.tags() != null ? request.tags() : List.of());
        product.setAttributes(request.attributes() != null ? request.attributes() : java.util.Map.of());
        product.setActive(request.isActive());
        product.setFeatured(request.isFeatured());

        Product saved = productRepository.save(product);
        return ProductResponse.from(saved);
    }

    public ProductResponse getProductById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return ProductResponse.from(product);
    }

    public PageResponse<ProductResponse> getAllProducts(String categoryId, String search, List<String> tags,
                                                         Boolean isActive, int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy != null ? sortBy : "createdAt").descending());

        Page<Product> productPage;

        if (search != null && !search.isBlank()) {
            productPage = productRepository.searchByText(search, pageable);
        } else if (categoryId != null && !categoryId.isBlank()) {
            if (isActive != null && isActive) {
                // Manual filtering needed since we want both filters
                productPage = productRepository.findByCategoryId(categoryId, pageable);
            } else {
                productPage = productRepository.findByCategoryId(categoryId, pageable);
            }
        } else if (tags != null && !tags.isEmpty()) {
            productPage = productRepository.findByTagsIn(tags, pageable);
        } else if (isActive != null && isActive) {
            productPage = productRepository.findByIsActiveTrue(pageable);
        } else {
            productPage = productRepository.findAll(pageable);
        }

        // Post-filter by isActive if specified
        List<ProductResponse> responses = productPage.getContent().stream()
                .filter(p -> isActive == null || p.isActive() == isActive)
                .map(ProductResponse::from)
                .toList();

        return new PageResponse<>(
                responses,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages()
        );
    }

    public void deleteProduct(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        product.setActive(false);
        productRepository.save(product);
    }

    public PageResponse<ProductResponse> getFeaturedProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productRepository.findByIsFeaturedTrueAndIsActiveTrue(pageable);

        List<ProductResponse> responses = productPage.getContent().stream()
                .map(ProductResponse::from)
                .toList();

        return new PageResponse<>(
                responses,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages()
        );
    }

    public List<ProductResponse> getProductsByCategoryId(String categoryId) {
        List<Product> products = productRepository.findByCategoryIdAndIsActiveTrue(categoryId);
        return products.stream()
                .map(ProductResponse::from)
                .toList();
    }
}
