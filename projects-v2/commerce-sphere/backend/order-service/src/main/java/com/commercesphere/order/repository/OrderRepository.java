package com.commercesphere.order.repository;
import com.commercesphere.order.document.Order;
import org.springframework.data.domain.Page;import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;
public interface OrderRepository extends MongoRepository<Order, String> {
    Page<Order> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    Optional<Order> findByOrderNumber(String orderNumber);
}
