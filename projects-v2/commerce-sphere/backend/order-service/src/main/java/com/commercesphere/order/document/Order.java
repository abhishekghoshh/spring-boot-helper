package com.commercesphere.order.document;
import lombok.*; import org.springframework.data.annotation.*; import org.springframework.data.mongodb.core.index.Indexed; import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal; import java.time.Instant; import java.util.List;
@Document("orders") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Order {
    @Id private String id; @Indexed(unique = true) private String orderNumber;
    @Indexed private String userId; private String userEmail;
    private List<OrderItem> items; private BigDecimal subtotal; private BigDecimal tax; private BigDecimal shipping; private BigDecimal total;
    private String status; private String shippingAddress; private String paymentMethod;
    @CreatedDate private Instant createdAt; @LastModifiedDate private Instant updatedAt;
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OrderItem { private String productId; private String productName; private BigDecimal price; private int quantity; }
}
