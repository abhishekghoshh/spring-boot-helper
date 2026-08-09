package com.commercesphere.catalog.service;

import com.commercesphere.catalog.document.Category;
import com.commercesphere.catalog.document.Product;
import com.commercesphere.catalog.dto.*;
import com.commercesphere.catalog.mapper.ProductMapper;
import com.commercesphere.catalog.repository.BrandRepository;
import com.commercesphere.catalog.repository.CategoryRepository;
import com.commercesphere.catalog.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
                          BrandRepository brandRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.productMapper = productMapper;
    }

    public PagedResponse<ProductDto> search(ProductSearchRequest request) {
        Sort sort = request.sortDir().equalsIgnoreCase("asc")
                ? Sort.by(request.sortBy()).ascending()
                : Sort.by(request.sortBy()).descending();
        PageRequest pageable = PageRequest.of(request.page(), request.size(), sort);

        Page<Product> page = productRepository.findWithFilters(
                request.active(), request.featured(), request.categoryId(),
                request.brandId(), request.minPrice(), request.maxPrice(), pageable);

        return new PagedResponse<>(
                page.getContent().stream().map(productMapper::toDto).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isLast());
    }

    @Cacheable(value = "products", key = "#id")
    public ProductDto getById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        return productMapper.toDto(product);
    }

    @CacheEvict(value = "products", allEntries = true)
    public ProductDto create(ProductCreateRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
        var brand = brandRepository.findById(request.brandId())
                .orElseThrow(() -> new RuntimeException("Brand not found"));

        Product product = productMapper.toEntity(request);
        product.setCategoryName(category.getName());
        product.setBrandName(brand.getName());
        product.setCreatedAt(Instant.now());
        product.setUpdatedAt(Instant.now());

        return productMapper.toDto(productRepository.save(product));
    }

    @CacheEvict(value = "products", key = "#id")
    public ProductDto update(String id, ProductCreateRequest request) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        existing.setSku(request.sku());
        existing.setName(request.name());
        existing.setDescription(request.description());
        existing.setPrice(request.price());
        existing.setCurrency(request.currency());
        existing.setCategoryId(request.categoryId());
        existing.setBrandId(request.brandId());
        existing.setActive(request.active());
        existing.setFeatured(request.featured());
        existing.setUpdatedAt(Instant.now());

        categoryRepository.findById(request.categoryId())
                .ifPresent(c -> existing.setCategoryName(c.getName()));
        brandRepository.findById(request.brandId())
                .ifPresent(b -> existing.setBrandName(b.getName()));

        return productMapper.toDto(productRepository.save(existing));
    }

    @CacheEvict(value = "products", allEntries = true)
    public void delete(String id) {
        productRepository.deleteById(id);
    }

    public PagedResponse<ProductDto> getFeatured(ProductSearchRequest request) {
        PageRequest pageable = PageRequest.of(request.page(), request.size());
        Page<Product> page = productRepository.findByFeaturedTrueAndActiveTrue(pageable);
        return new PagedResponse<>(
                page.getContent().stream().map(productMapper::toDto).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isLast());
    }
}
