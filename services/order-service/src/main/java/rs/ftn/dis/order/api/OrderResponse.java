package rs.ftn.dis.order.api;

import rs.ftn.dis.order.domain.Order;
import rs.ftn.dis.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String id,
        String customerEmail,
        OrderStatus status,
        String statusReason,
        BigDecimal total,
        Instant createdAt,
        List<Item> items
) {

    public record Item(String productId, String productName, BigDecimal unitPrice, int quantity) {
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerEmail(),
                order.getStatus(),
                order.getStatusReason(),
                order.getTotal(),
                order.getCreatedAt(),
                order.getItems().stream()
                        .map(i -> new Item(i.getProductId(), i.getProductName(), i.getUnitPrice(), i.getQuantity()))
                        .toList());
    }
}
