package com.commercesphere.catalog.service;

import com.commercesphere.catalog.document.Category;
import com.commercesphere.catalog.dto.CategoryDto;
import com.commercesphere.catalog.mapper.CategoryMapper;
import com.commercesphere.catalog.repository.CategoryRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Cacheable(value = "categories")
    public List<CategoryDto> getAll() {
        return categoryRepository.findAll().stream().map(categoryMapper::toDto).toList();
    }

    @Cacheable(value = "categories", key = "#id")
    public CategoryDto getById(String id) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        return categoryMapper.toDto(cat);
    }

    @CacheEvict(value = "categories", allEntries = true)
    public CategoryDto create(CategoryDto dto) {
        Category category = categoryMapper.toEntity(dto);
        return categoryMapper.toDto(categoryRepository.save(category));
    }

    @CacheEvict(value = "categories", allEntries = true)
    public CategoryDto update(String id, CategoryDto dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        category.setName(dto.name());
        category.setSlug(dto.slug());
        category.setDescription(dto.description());
        category.setImageUrl(dto.imageUrl());
        return categoryMapper.toDto(categoryRepository.save(category));
    }

    @CacheEvict(value = "categories", allEntries = true)
    public void delete(String id) {
        categoryRepository.deleteById(id);
    }
}
