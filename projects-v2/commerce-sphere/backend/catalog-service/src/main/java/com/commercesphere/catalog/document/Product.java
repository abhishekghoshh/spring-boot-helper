package com.commercesphere.catalog.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Document(collection = "products")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Product {
    @Id
    private String id;
    private String sku;
    @TextIndexed
    private String name;
    @TextIndexed
    private String description;
    private BigDecimal price;
    private String currency;
    @Indexed
    private String categoryId;
    private String categoryName;
    @Indexed
    private String brandId;
    private String brandName;
    private List<String> imageUrls;
    private List<ProductSpecification> specifications;
    private boolean active;
    private boolean featured;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProductSpecification {
        private String key;
        private String value;
    }
}
