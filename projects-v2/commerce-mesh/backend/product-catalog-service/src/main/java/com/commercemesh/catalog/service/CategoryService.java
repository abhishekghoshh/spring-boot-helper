package com.commercemesh.catalog.service;

import com.commercemesh.catalog.document.Category;
import com.commercemesh.catalog.dto.CategoryRequest;
import com.commercemesh.catalog.dto.CategoryResponse;
import com.commercemesh.catalog.exception.ResourceNotFoundException;
import com.commercemesh.catalog.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.findBySlug(request.slug()).isPresent()) {
            throw new IllegalArgumentException("Category with slug '" + request.slug() + "' already exists");
        }

        Category category = new Category();
        category.setName(request.name());
        category.setSlug(request.slug());
        category.setDescription(request.description());
        category.setImage(request.image());
        category.setParentId(request.parentId());
        category.setActive(request.isActive());
        category.setSortOrder(request.sortOrder());

        Category saved = categoryRepository.save(category);
        return CategoryResponse.from(saved);
    }

    public CategoryResponse updateCategory(String id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        category.setName(request.name());
        category.setSlug(request.slug());
        category.setDescription(request.description());
        category.setImage(request.image());
        category.setParentId(request.parentId());
        category.setActive(request.isActive());
        category.setSortOrder(request.sortOrder());

        Category saved = categoryRepository.save(category);
        return CategoryResponse.from(saved);
    }

    public List<CategoryResponse> getAllCategories() {
        List<Category> categories = categoryRepository.findByIsActiveTrue();
        return categories.stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public CategoryResponse getCategoryById(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return CategoryResponse.from(category);
    }

    public List<CategoryResponse> getSubcategories(String parentId) {
        List<Category> subcategories = categoryRepository.findByParentIdAndIsActiveTrue(parentId);
        return subcategories.stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public void deleteCategory(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        List<Category> subcategories = categoryRepository.findByParentId(id);
        if (!subcategories.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot delete category with subcategories. Remove subcategories first.");
        }

        categoryRepository.delete(category);
    }

    public List<CategoryResponse> getCategoryTree() {
        List<Category> allCategories = categoryRepository.findByIsActiveTrue();

        Map<String, List<Category>> childrenByParent = allCategories.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Category::getParentId));

        List<Category> rootCategories = allCategories.stream()
                .filter(c -> c.getParentId() == null)
                .toList();

        return rootCategories.stream()
                .map(root -> buildTree(root, childrenByParent))
                .toList();
    }

    private CategoryResponse buildTree(Category category, Map<String, List<Category>> childrenByParent) {
        List<Category> children = childrenByParent.getOrDefault(category.getId(), List.of());
        List<CategoryResponse> childResponses = children.stream()
                .map(child -> buildTree(child, childrenByParent))
                .toList();
        return CategoryResponse.from(category, childResponses);
    }
}
