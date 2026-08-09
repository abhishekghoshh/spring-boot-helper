package com.commercemesh.order.service;

import com.commercemesh.order.client.InventoryClient;
import com.commercemesh.order.client.PaymentClient;
import com.commercemesh.order.dto.CreateOrderRequest;
import com.commercemesh.order.dto.OrderItemRequest;
import com.commercemesh.order.dto.OrderResponse;
import com.commercemesh.order.dto.OrderStatusUpdateRequest;
import com.commercemesh.order.dto.PageResponse;
import com.commercemesh.order.dto.ShippingAddress;
import com.commercemesh.order.exception.InsufficientStockException;
import com.commercemesh.order.exception.OrderStatusTransitionException;
import com.commercemesh.order.exception.ResourceNotFoundException;
import com.commercemesh.order.model.Order;
import com.commercemesh.order.model.OrderItem;
import com.commercemesh.order.model.OrderStatus;
import com.commercemesh.order.repository.OrderEventRepository;
import com.commercemesh.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventRepository orderEventRepository;

    @Mock
    private OrderEventPublisher eventPublisher;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private PaymentClient paymentClient;

    @InjectMocks
    private OrderService orderService;

    private Order order;
    private CreateOrderRequest createOrderRequest;
    private ShippingAddress shippingAddress;

    @BeforeEach
    void setUp() {
        shippingAddress = new ShippingAddress("123 Main St", "Apt 4B", "New York", "NY", "US", "10001", "555-1234");

        OrderItemRequest item = new OrderItemRequest(1L, "Test Product", new BigDecimal("49.99"), 2);
        createOrderRequest = new CreateOrderRequest(List.of(item), shippingAddress, "Please deliver fast");

        order = new Order();
        order.setId(100L);
        order.setUserId(200L);
        order.setShippingAddress(new com.commercemesh.order.model.Address(
                "123 Main St", "Apt 4B", "New York", "NY", "US", "10001", "555-1234"));
        order.setStatus(OrderStatus.PENDING);
        order.setNotes("Please deliver fast");

        OrderItem orderItem = new OrderItem(1L, "Test Product", new BigDecimal("49.99"), 2);
        order.addItem(orderItem);
        order.recalculate();
    }

    @Nested
    @DisplayName("createOrder")
    class CreateOrderTests {

        @BeforeEach
        void setUpStockCheck() {
            when(inventoryClient.checkStock(1L))
                    .thenReturn(Map.of("available", true, "quantity", 100));
        }

        @Test
        @DisplayName("should create order with correct total including tax")
        void createOrderCalculatesCorrectTotal() {
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                o.setId(100L);
                // Simulate auto-generated order number from @PrePersist
                if (o.getOrderNumber() == null) {
                    o.setOrderNumber("uuid-123");
                }
                return o;
            });

            OrderResponse response = orderService.createOrder(200L, createOrderRequest);

            assertThat(response.getUserId()).isEqualTo(200L);
            assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(response.getSubtotal()).isEqualTo(new BigDecimal("99.98")); // 49.99 * 2
            assertThat(response.getTax()).isEqualTo(new BigDecimal("7.9984")); // 99.98 * 0.08
            assertThat(response.getTotal()).isEqualTo(new BigDecimal("107.9784")); // subtotal + tax
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).productName()).isEqualTo("Test Product");

            verify(eventPublisher).publishOrderCreated(eq(100L), any(), eq(200L));
        }

        @Test
        @DisplayName("should create order with multiple items and correct totals")
        void createOrderWithMultipleItemsCalculatesCorrectly() {
            OrderItemRequest item1 = new OrderItemRequest(1L, "Product A", new BigDecimal("10.00"), 3);
            OrderItemRequest item2 = new OrderItemRequest(2L, "Product B", new BigDecimal("25.00"), 1);
            CreateOrderRequest multiRequest = new CreateOrderRequest(List.of(item1, item2), shippingAddress, null);

            when(inventoryClient.checkStock(1L)).thenReturn(Map.of("available", true, "quantity", 100));
            when(inventoryClient.checkStock(2L)).thenReturn(Map.of("available", true, "quantity", 50));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                o.setId(101L);
                return o;
            });

            OrderResponse response = orderService.createOrder(200L, multiRequest);

            // subtotal: 10*3 + 25*1 = 55, tax: 55*0.08 = 4.4, total: 59.4
            assertThat(response.getSubtotal()).isEqualByComparingTo(new BigDecimal("55.0000"));
            assertThat(response.getTotal()).isEqualByComparingTo(new BigDecimal("59.4000"));
        }

        @Test
        @DisplayName("should throw InsufficientStockException when product stock is not available")
        void createOrderWithInsufficientStockThrowsException() {
            when(inventoryClient.checkStock(1L))
                    .thenReturn(Map.of("available", false, "quantity", 0));

            assertThatThrownBy(() -> orderService.createOrder(200L, createOrderRequest))
                    .isInstanceOf(InsufficientStockException.class);

            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw InsufficientStockException when requested quantity exceeds available")
        void createOrderWithQuantityExceedingStockThrowsException() {
            when(inventoryClient.checkStock(1L))
                    .thenReturn(Map.of("available", true, "quantity", 1));

            assertThatThrownBy(() -> orderService.createOrder(200L, createOrderRequest))
                    .isInstanceOf(InsufficientStockException.class);

            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("should proceed when inventory service is unavailable (resilience)")
        void createOrderProceedsWhenInventoryUnavailable() {
            when(inventoryClient.checkStock(1L))
                    .thenThrow(new RuntimeException("Service unavailable"));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                o.setId(102L);
                return o;
            });

            OrderResponse response = orderService.createOrder(200L, createOrderRequest);

            assertThat(response.getId()).isEqualTo(102L);
            assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("getOrdersByUser")
    class GetOrdersByUserTests {

        @Test
        @DisplayName("should return paginated orders for a user")
        void returnsPaginatedResults() {
            Order order2 = new Order();
            order2.setId(101L);
            order2.setUserId(200L);

            Page<Order> page = new PageImpl<>(List.of(order, order2), PageRequest.of(0, 10), 2);
            when(orderRepository.findByUserId(eq(200L), any())).thenReturn(page);

            PageResponse<OrderResponse> response = orderService.getOrdersByUser(200L, PageRequest.of(0, 10));

            assertThat(response.getContent()).hasSize(2);
            assertThat(response.getTotalElements()).isEqualTo(2);
            assertThat(response.getTotalPages()).isEqualTo(1);
            assertThat(response.getPage()).isEqualTo(0);
            assertThat(response.getSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("should return empty page when user has no orders")
        void returnsEmptyPageForUserWithNoOrders() {
            Page<Order> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
            when(orderRepository.findByUserId(eq(999L), any())).thenReturn(emptyPage);

            PageResponse<OrderResponse> response = orderService.getOrdersByUser(999L, PageRequest.of(0, 10));

            assertThat(response.getContent()).isEmpty();
            assertThat(response.getTotalElements()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getOrderById")
    class GetOrderByIdTests {

        @Test
        @DisplayName("should return order when found")
        void returnsOrderWhenFound() {
            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

            OrderResponse response = orderService.getOrderById(100L);

            assertThat(response.getId()).isEqualTo(100L);
            assertThat(response.getUserId()).isEqualTo(200L);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when order not found")
        void throwsExceptionWhenNotFound() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Order")
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("updateOrderStatus")
    class UpdateOrderStatusTests {

        @Test
        @DisplayName("should transition PENDING -> CONFIRMED")
        void transitionPendingToConfirmed() {
            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(OrderStatus.CONFIRMED, null);
            OrderResponse response = orderService.updateOrderStatus(100L, request);

            assertThat(response.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
            verify(eventPublisher).publishOrderConfirmed(eq(100L), any(), eq(200L));
        }

        @Test
        @DisplayName("should transition CONFIRMED -> PROCESSING")
        void transitionConfirmedToProcessing() {
            order.setStatus(OrderStatus.CONFIRMED);
            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(OrderStatus.PROCESSING, null);
            OrderResponse response = orderService.updateOrderStatus(100L, request);

            assertThat(response.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        }

        @Test
        @DisplayName("should throw OrderStatusTransitionException for invalid transition DELIVERED -> PENDING")
        void invalidTransitionDeliveredToPendingThrowsException() {
            order.setStatus(OrderStatus.DELIVERED);
            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

            OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(OrderStatus.PENDING, null);

            assertThatThrownBy(() -> orderService.updateOrderStatus(100L, request))
                    .isInstanceOf(OrderStatusTransitionException.class)
                    .hasMessageContaining("DELIVERED")
                    .hasMessageContaining("PENDING");
        }

        @Test
        @DisplayName("should throw OrderStatusTransitionException for invalid transition from CANCELLED")
        void invalidTransitionFromCancelledThrowsException() {
            order.setStatus(OrderStatus.CANCELLED);
            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

            OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(OrderStatus.CONFIRMED, null);

            assertThatThrownBy(() -> orderService.updateOrderStatus(100L, request))
                    .isInstanceOf(OrderStatusTransitionException.class);
        }

        @Test
        @DisplayName("should accept same-status transition as no-op")
        void sameStatusTransitionIsNoOp() {
            order.setStatus(OrderStatus.PROCESSING);
            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(OrderStatus.PROCESSING, null);
            OrderResponse response = orderService.updateOrderStatus(100L, request);

            assertThat(response.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        }
    }

    @Nested
    @DisplayName("cancelOrder")
    class CancelOrderTests {

        @Test
        @DisplayName("should cancel a PENDING order")
        void cancelsPendingOrder() {
            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            OrderResponse response = orderService.cancelOrder(100L, 200L);

            assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(eventPublisher).publishOrderCancelled(eq(100L), any(), eq(200L));
        }

        @Test
        @DisplayName("should cancel a CONFIRMED order")
        void cancelsConfirmedOrder() {
            order.setStatus(OrderStatus.CONFIRMED);
            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            OrderResponse response = orderService.cancelOrder(100L, 200L);

            assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("should throw OrderStatusTransitionException for SHIPPED orders")
        void cancelShippedOrderThrowsException() {
            order.setStatus(OrderStatus.SHIPPED);
            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(100L, 200L))
                    .isInstanceOf(OrderStatusTransitionException.class)
                    .hasMessageContaining("SHIPPED")
                    .hasMessageContaining("CANCELLED");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when cancelling another user's order")
        void cancelAnotherUsersOrderThrowsException() {
            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(100L, 999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("only cancel your own orders");
        }
    }
}
