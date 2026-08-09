package com.commercesphere.inventory.document;
import lombok.*; import org.springframework.data.annotation.*; import org.springframework.data.mongodb.core.index.CompoundIndex; import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
@Document("inventory") @CompoundIndex(def = "{'productId':1,'warehouseId':1}", unique = true)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Inventory {
    @Id private String id; private String productId; private String productName;
    private String warehouseId; private String warehouseName; private int quantity; private int reservedQuantity;
    @CreatedDate private Instant createdAt; @LastModifiedDate private Instant updatedAt;
    public int getAvailableQuantity() { return quantity - reservedQuantity; }
}
