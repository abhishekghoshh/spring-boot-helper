package com.commercesphere.catalog.repository;

import com.commercesphere.catalog.document.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Product> findByCategoryId(String categoryId, Pageable pageable);
    Page<Product> findByBrandId(String brandId, Pageable pageable);
    Page<Product> findByActiveTrue(Pageable pageable);
    Page<Product> findByFeaturedTrueAndActiveTrue(Pageable pageable);
    List<Product> findByCategoryId(String categoryId);

    @Query("{'$and':[" +
            "?#{ [0] == null ? {$where: 'true'} : {active: [0]} }," +
            "?#{ [1] == null ? {$where: 'true'} : {featured: [1]} }," +
            "?#{ [2] == null ? {$where: 'true'} : {categoryId: [2]} }," +
            "?#{ [3] == null ? {$where: 'true'} : {brandId: [3]} }," +
            "?#{ [4] == null ? {$where: 'true'} : {price: {$gte: [4]}} }," +
            "?#{ [5] == null ? {$where: 'true'} : {price: {$lte: [5]}} }" +
            "]}")
    Page<Product> findWithFilters(Boolean active, Boolean featured, String categoryId,
                                   String brandId, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
}
