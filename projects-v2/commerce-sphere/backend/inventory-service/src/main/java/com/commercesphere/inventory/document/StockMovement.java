package com.commercesphere.inventory.document;
import lombok.*; import org.springframework.data.annotation.*; import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
@Document("stockMovements") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StockMovement {
    @Id private String id; private String productId; private String warehouseId; private String type;
    private int quantity; private String referenceId; private String reason; @CreatedDate private Instant createdAt;
}
