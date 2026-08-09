package com.commercemesh.search.repository;

import com.commercemesh.search.document.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSearchRepository extends MongoRepository<ProductDocument, String> {

    /**
     * Full-text search using MongoDB $text $search operator, sorted by text score.
     * Filters by isActive = true only.
     */
    @Query("{ '$text': { '$search': ?0 }, 'isActive': true }")
    Page<ProductDocument> searchByKeyword(String keyword, Pageable pageable);

    /**
     * Find active products by category with pagination.
     */
    Page<ProductDocument> findByCategoryAndIsActiveTrue(String category, Pageable pageable);

    /**
     * Autocomplete: find top 10 product names matching the keyword (prefix-style, case-insensitive).
     */
    @Query(value = "{ 'name': { '$regex': ?0, '$options': 'i' }, 'isActive': true }",
           fields = "{ 'name': 1 }")
    List<ProductDocument> findByNameRegex(String regex, Pageable pageable);

    /**
     * Faceted aggregation: category counts for a set of product IDs.
     */
    @Aggregation(pipeline = {
            "{ '$match': { '_id': { '$in': ?0 } } }",
            "{ '$group': { '_id': '$category', 'count': { '$sum': 1 } } }"
    })
    List<CategoryCount> countByCategory(List<String> productIds);

    /**
     * Faceted aggregation: price range counts.
     */
    @Aggregation(pipeline = {
            "{ '$match': { '_id': { '$in': ?0 } } }",
            "{ '$bucket': { 'groupBy': '$price', 'boundaries': ?1, 'default': 'other', 'output': { 'count': { '$sum': 1 } } } }"
    })
    List<PriceRangeCount> countByPriceRange(List<String> productIds, List<Double> boundaries);

    /**
     * Check if collection has any documents.
     */
    default boolean isEmpty() {
        return count() == 0;
    }

    record CategoryCount(String id, long count) {
    }

    record PriceRangeCount(String id, long count) {
    }
}
