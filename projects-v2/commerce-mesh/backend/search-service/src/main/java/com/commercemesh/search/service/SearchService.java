package com.commercemesh.search.service;

import com.commercemesh.search.document.ProductDocument;
import com.commercemesh.search.dto.AutoCompleteResponse;
import com.commercemesh.search.dto.ProductDocumentResponse;
import com.commercemesh.search.dto.SearchRequest;
import com.commercemesh.search.dto.SearchResponse;
import com.commercemesh.search.exception.ResourceNotFoundException;
import com.commercemesh.search.repository.ProductSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final ProductSearchRepository repository;
    private final MongoTemplate mongoTemplate;

    public SearchService(ProductSearchRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Perform full-text search with optional category and price filters.
     */
    public SearchResponse search(SearchRequest request) {
        Pageable pageable = buildPageable(request);
        Page<ProductDocument> page;

        if (request.keyword() != null && !request.keyword().isBlank()) {
            page = textSearch(request, pageable);
        } else {
            page = filterSearch(request, pageable);
        }

        List<ProductDocumentResponse> results = page.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        Map<String, Long> facets = computeFacets(page.getContent());
        Map<String, Long> priceRanges = computePriceRanges(page.getContent());

        return new SearchResponse(
                results,
                page.getTotalElements(),
                page.getNumber(),
                page.getSize(),
                page.getTotalPages(),
                facets,
                priceRanges
        );
    }

    /**
     * Text search using MongoDB $text operator with additional filters.
     */
    private Page<ProductDocument> textSearch(SearchRequest request, Pageable pageable) {
        TextCriteria textCriteria = TextCriteria.forDefaultLanguage().matching(request.keyword());

        Query query = TextQuery.queryText(textCriteria)
                .sortByScore()
                .with(pageable);

        // Add filters
        List<Criteria> filters = new ArrayList<>();
        filters.add(Criteria.where("isActive").is(true));

        if (request.category() != null && !request.category().isBlank()) {
            filters.add(Criteria.where("category").is(request.category()));
        }
        if (request.minPrice() != null && request.minPrice().compareTo(BigDecimal.ZERO) > 0) {
            filters.add(Criteria.where("price").gte(request.minPrice().doubleValue()));
        }
        if (request.maxPrice() != null && request.maxPrice().compareTo(BigDecimal.ZERO) > 0) {
            filters.add(Criteria.where("price").lte(request.maxPrice().doubleValue()));
        }
        if (request.minRating() != null && request.minRating() > 0) {
            filters.add(Criteria.where("averageRating").gte(request.minRating()));
        }

        for (Criteria filter : filters) {
            query.addCriteria(filter);
        }

        long total = mongoTemplate.count(Query.of(query).limit(0).skip(0), ProductDocument.class);
        List<ProductDocument> docs = mongoTemplate.find(query, ProductDocument.class);

        return new PageImpl<>(docs, pageable, total);
    }

    /**
     * Filter-only search (no keyword) — used for category browsing.
     */
    private Page<ProductDocument> filterSearch(SearchRequest request, Pageable pageable) {
        Query query = new Query().with(pageable);
        query.addCriteria(Criteria.where("isActive").is(true));

        if (request.category() != null && !request.category().isBlank()) {
            query.addCriteria(Criteria.where("category").is(request.category()));
        }
        if (request.minPrice() != null && request.minPrice().compareTo(BigDecimal.ZERO) > 0) {
            query.addCriteria(Criteria.where("price").gte(request.minPrice().doubleValue()));
        }
        if (request.maxPrice() != null && request.maxPrice().compareTo(BigDecimal.ZERO) > 0) {
            query.addCriteria(Criteria.where("price").lte(request.maxPrice().doubleValue()));
        }
        if (request.minRating() != null && request.minRating() > 0) {
            query.addCriteria(Criteria.where("averageRating").gte(request.minRating()));
        }

        if (request.sortBy() != null) {
            switch (request.sortBy()) {
                case PRICE_ASC -> query.with(Sort.by(Sort.Direction.ASC, "price"));
                case PRICE_DESC -> query.with(Sort.by(Sort.Direction.DESC, "price"));
                case RATING -> query.with(Sort.by(Sort.Direction.DESC, "averageRating"));
                case NEWEST -> query.with(Sort.by(Sort.Direction.DESC, "createdAt"));
                // RELEVANCE has no meaning without a text query
            }
        }

        long total = mongoTemplate.count(Query.of(query).limit(0).skip(0), ProductDocument.class);
        List<ProductDocument> docs = mongoTemplate.find(query, ProductDocument.class);

        return new PageImpl<>(docs, pageable, total);
    }

    /**
     * Autocomplete: return top 10 product name suggestions matching the keyword prefix.
     */
    public AutoCompleteResponse autocomplete(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return new AutoCompleteResponse(List.of());
        }

        String escaped = keyword.replaceAll("[.*+?^${}()|\\[\\]\\\\]", "\\\\$0");
        String regex = "^" + escaped;

        Pageable pageable = PageRequest.of(0, 10);
        List<ProductDocument> matches = repository.findByNameRegex(regex, pageable);

        List<String> suggestions = matches.stream()
                .map(ProductDocument::getName)
                .distinct()
                .collect(Collectors.toList());

        return new AutoCompleteResponse(suggestions);
    }

    /**
     * Index (save or update) a single product document.
     */
    public ProductDocument indexProduct(ProductDocument product) {
        if (product.getCreatedAt() == null) {
            product.setCreatedAt(Instant.now());
        }
        product.setActive(true);
        return repository.save(product);
    }

    /**
     * Remove a product from the search index by its ID.
     */
    public void removeProductFromIndex(String productId) {
        if (!repository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", "id", productId);
        }
        repository.deleteById(productId);
        log.info("Removed product {} from search index.", productId);
    }

    /**
     * Reindex all products: bulk save a list of product documents.
     */
    public void reindexAll(List<ProductDocument> products) {
        repository.deleteAll();
        repository.saveAll(products);
        log.info("Reindexed {} products.", products.size());
    }

    /**
     * Compute category facets from search results.
     */
    private Map<String, Long> computeFacets(List<ProductDocument> docs) {
        return docs.stream()
                .collect(Collectors.groupingBy(
                        doc -> doc.getCategory() != null ? doc.getCategory() : "Uncategorized",
                        Collectors.counting()
                ));
    }

    /**
     * Compute price range distribution from search results.
     */
    private Map<String, Long> computePriceRanges(List<ProductDocument> docs) {
        Map<String, Long> ranges = new LinkedHashMap<>();
        ranges.put("$0 - $25", 0L);
        ranges.put("$25 - $50", 0L);
        ranges.put("$50 - $100", 0L);
        ranges.put("$100 - $200", 0L);
        ranges.put("$200+", 0L);

        for (ProductDocument doc : docs) {
            double price = doc.getPrice() != null ? doc.getPrice().doubleValue() : 0.0;
            if (price < 25) {
                ranges.merge("$0 - $25", 1L, Long::sum);
            } else if (price < 50) {
                ranges.merge("$25 - $50", 1L, Long::sum);
            } else if (price < 100) {
                ranges.merge("$50 - $100", 1L, Long::sum);
            } else if (price < 200) {
                ranges.merge("$100 - $200", 1L, Long::sum);
            } else {
                ranges.merge("$200+", 1L, Long::sum);
            }
        }
        return ranges;
    }

    private Pageable buildPageable(SearchRequest request) {
        Sort sort = Sort.unsorted();
        if (request.sortBy() != null) {
            switch (request.sortBy()) {
                case PRICE_ASC -> sort = Sort.by(Sort.Direction.ASC, "price");
                case PRICE_DESC -> sort = Sort.by(Sort.Direction.DESC, "price");
                case RATING -> sort = Sort.by(Sort.Direction.DESC, "averageRating");
                case NEWEST -> sort = Sort.by(Sort.Direction.DESC, "createdAt");
                // RELEVANCE is handled by text score in textSearch
            }
        }
        return PageRequest.of(request.page(), request.size(), sort);
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

    /**
     * Minimal custom Page implementation so we can use MongoTemplate queries
     * while still returning Spring Data Page objects.
     */
    private static class PageImpl<T> implements Page<T> {
        private final List<T> content;
        private final Pageable pageable;
        private final long total;

        PageImpl(List<T> content, Pageable pageable, long total) {
            this.content = content;
            this.pageable = pageable;
            this.total = total;
        }

        @Override
        public int getTotalPages() {
            int size = pageable.getPageSize();
            return size == 0 ? 1 : (int) Math.ceil((double) total / (double) size);
        }

        @Override
        public long getTotalElements() { return total; }

        @Override
        public int getNumber() { return pageable.getPageNumber(); }

        @Override
        public int getSize() { return pageable.getPageSize(); }

        @Override
        public int getNumberOfElements() { return content.size(); }

        @Override
        public List<T> getContent() { return Collections.unmodifiableList(content); }

        @Override
        public boolean hasContent() { return !content.isEmpty(); }

        @Override
        public Sort getSort() { return pageable.getSort(); }

        @Override
        public boolean isFirst() { return !pageable.hasPrevious(); }

        @Override
        public boolean isLast() {
            return getNumber() >= getTotalPages() - 1;
        }

        @Override
        public boolean hasNext() { return !isLast(); }

        @Override
        public boolean hasPrevious() { return pageable.hasPrevious(); }

        @Override
        public Pageable nextPageable() { return isLast() ? Pageable.unpaged() : pageable.next(); }

        @Override
        public Pageable previousPageable() {
            return pageable.hasPrevious() ? pageable.previousOrFirst() : Pageable.unpaged();
        }

        @Override
        public <U> Page<U> map(java.util.function.Function<? super T, ? extends U> converter) {
            List<U> converted = new ArrayList<>(content.size());
            for (T item : content) {
                converted.add(converter.apply(item));
            }
            return new PageImpl<>(converted, pageable, total);
        }

        @Override
        public Iterator<T> iterator() { return content.iterator(); }
    }
}
