package com.commercesphere.inventory.document;
import lombok.*; import org.springframework.data.annotation.*; import org.springframework.data.mongodb.core.index.Indexed; import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
@Document("warehouses") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Warehouse {
    @Id private String id; @Indexed(unique = true) private String name; @Indexed(unique = true) private String code;
    private Address address; private boolean active; @CreatedDate private Instant createdAt; @LastModifiedDate private Instant updatedAt;
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Address { private String street; private String city; private String state; private String zipCode; private String country; }
}
