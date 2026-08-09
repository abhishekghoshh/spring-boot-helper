package com.commercemesh.catalog.repository;

import com.commercemesh.catalog.document.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    Page<Product> findByCategoryId(String categoryId, Pageable pageable);

    Page<Product> findByIsActiveTrue(Pageable pageable);

    Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String name, String description, Pageable pageable);

    Page<Product> findByTagsIn(List<String> tags, Pageable pageable);

    Page<Product> findByIsFeaturedTrueAndIsActiveTrue(Pageable pageable);

    @Query("{ $text: { $search: ?0 } }")
    Page<Product> searchByText(String searchText, Pageable pageable);

    List<Product> findByCategoryIdAndIsActiveTrue(String categoryId);
}
