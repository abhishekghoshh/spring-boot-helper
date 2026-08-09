package com.commercemesh.catalog.repository;

import com.commercemesh.catalog.document.Category;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends MongoRepository<Category, String> {

    Optional<Category> findBySlug(String slug);

    List<Category> findByIsActiveTrue();

    List<Category> findByParentId(String parentId);

    List<Category> findByParentIdAndIsActiveTrue(String parentId);

    List<Category> findByParentIdIsNull();

    List<Category> findByParentIdIsNullAndIsActiveTrue();
}
