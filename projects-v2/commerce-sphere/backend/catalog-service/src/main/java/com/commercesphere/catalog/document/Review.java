package com.commercesphere.catalog.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "reviews")
@CompoundIndex(def = "{'productId':1,'userId':1}", unique = true)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Review {
    @Id
    private String id;
    @Indexed
    private String productId;
    @Indexed
    private String userId;
    private String userName;
    private int rating;
    private String comment;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
}
