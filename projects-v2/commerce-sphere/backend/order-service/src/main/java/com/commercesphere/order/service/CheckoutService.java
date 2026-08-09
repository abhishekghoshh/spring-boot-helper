package com.commercesphere.order.service;
import com.commercesphere.order.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.util.*;

@Service
public class CheckoutService {
    private final RestTemplate restTemplate;
    private final OrderService orderService;
    private final String cartServiceUrl;
    private final String inventoryServiceUrl;

    public CheckoutService(RestTemplate restTemplate, OrderService orderService,
                           @Value("${app.cart.url}") String cartServiceUrl,
                           @Value("${app.inventory.url}") String inventoryServiceUrl) {
        this.restTemplate = restTemplate;
        this.orderService = orderService;
        this.cartServiceUrl = cartServiceUrl;
        this.inventoryServiceUrl = inventoryServiceUrl;
    }

    public OrderDto checkout(CheckoutRequest request) {
        var cartResp = restTemplate.exchange(cartServiceUrl + "/api/cart/" + request.cartId(), HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {});
        Map<String, Object> cart = cartResp.getBody();
        if (cart == null || ((List<?>) cart.get("items")).isEmpty()) throw new RuntimeException("Cart is empty");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawItems = (List<Map<String, Object>>) cart.get("items");
        List<OrderItemDto> items = rawItems.stream().map(i -> new OrderItemDto(
                (String) i.get("productId"), (String) i.get("productName"),
                new BigDecimal(i.get("price").toString()), ((Number) i.get("quantity")).intValue())).toList();
        BigDecimal subtotal = new BigDecimal(cart.get("totalAmount").toString());

        for (var item : items) {
            Map<String, Object> checkBody = Map.of("productId", item.productId(), "quantity", item.quantity());
            restTemplate.postForEntity(inventoryServiceUrl + "/api/inventory/check", checkBody, Map.class);
        }

        OrderDto order = orderService.placeOrder(request, items, subtotal);

        for (var item : items) {
            Map<String, Object> reserveBody = Map.of("productId", item.productId(), "orderId", order.id(), "quantity", item.quantity());
            restTemplate.postForEntity(inventoryServiceUrl + "/api/inventory/reserve", reserveBody, Map.class);
        }

        restTemplate.delete(cartServiceUrl + "/api/cart/" + request.cartId());
        restTemplate.exchange(cartServiceUrl + "/api/cart/" + request.userId() + "/save-to-history", HttpMethod.POST, null, Void.class);

        return order;
    }
}
