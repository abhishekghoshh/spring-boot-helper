package com.commercesphere.order.service;
import com.commercesphere.order.document.Order;
import com.commercesphere.order.dto.*;
import com.commercesphere.order.event.*;
import com.commercesphere.order.repository.OrderRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class OrderService {
    private final OrderRepository orderRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final ReentrantLock lock = new ReentrantLock();

    public OrderService(OrderRepository orderRepo, ApplicationEventPublisher eventPublisher) {
        this.orderRepo = orderRepo; this.eventPublisher = eventPublisher;
    }

    public OrderDto placeOrder(CheckoutRequest request, List<OrderItemDto> items, BigDecimal subtotal) {
        lock.lock();
        try {
            BigDecimal tax = subtotal.multiply(new BigDecimal("0.08"));
            BigDecimal shipping = subtotal.compareTo(new BigDecimal("50")) > 0 ? BigDecimal.ZERO : new BigDecimal("9.99");
            BigDecimal total = subtotal.add(tax).add(shipping);
            String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            Order order = Order.builder()
                    .orderNumber(orderNumber).userId(request.userId()).userEmail(request.userEmail())
                    .items(items.stream().map(i -> Order.OrderItem.builder().productId(i.productId())
                            .productName(i.productName()).price(i.price()).quantity(i.quantity()).build()).toList())
                    .subtotal(subtotal).tax(tax).shipping(shipping).total(total)
                    .status("CONFIRMED").shippingAddress(request.shippingAddress())
                    .createdAt(Instant.now()).updatedAt(Instant.now())
                    .build();

            Order saved = orderRepo.save(order);
            eventPublisher.publishEvent(new OrderPlacedEvent(saved));
            return toDto(saved);
        } finally { lock.unlock(); }
    }

    public List<OrderDto> getUserOrders(String userId, int page, int size) {
        var pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderRepo.findByUserIdOrderByCreatedAtDesc(userId, pr).stream().map(this::toDto).toList();
    }

    public OrderDto getOrder(String orderId) {
        return toDto(orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found")));
    }

    public OrderDto cancelOrder(String orderId) {
        Order order = orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        if (!"CONFIRMED".equals(order.getStatus())) throw new RuntimeException("Only confirmed orders can be cancelled");
        order.setStatus("CANCELLED");
        order.setUpdatedAt(Instant.now());
        Order saved = orderRepo.save(order);
        eventPublisher.publishEvent(new OrderCancelledEvent(saved));
        return toDto(saved);
    }

    private OrderDto toDto(Order o) {
        return new OrderDto(o.getId(), o.getOrderNumber(), o.getUserId(), o.getUserEmail(),
                o.getItems().stream().map(i -> new OrderItemDto(i.getProductId(), i.getProductName(), i.getPrice(), i.getQuantity())).toList(),
                o.getSubtotal(), o.getTax(), o.getShipping(), o.getTotal(), o.getStatus(), o.getShippingAddress(), o.getCreatedAt());
    }
}
