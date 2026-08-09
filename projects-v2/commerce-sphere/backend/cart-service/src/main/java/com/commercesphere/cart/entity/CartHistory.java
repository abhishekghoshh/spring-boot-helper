package com.commercesphere.cart.entity;
import lombok.AllArgsConstructor;import lombok.Builder;import lombok.Data;import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;import java.time.Instant;import java.util.List;
@Document("cartHistory")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CartHistory {
    @Id private String id; private String userId; private List<CartItem> items;
    private BigDecimal totalAmount; private Instant createdAt;
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CartItem { private String productId; private String productName; private BigDecimal price; private int quantity; }
}
