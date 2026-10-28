package rs.ftn.dis.order.events;

import rs.ftn.dis.order.domain.Order;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dogadjaj "porudzbina je napravljena". Objavljuje se na RabbitMQ (exchange "orders").
 * Slusaju ga payment-service (naplata) i notification-service (obavestenje kupcu).
 *
 * Svaki servis ima SVOJU kopiju ove klase: servisi dele samo format poruke (JSON),
 * ne i kod, pa mogu da se razvijaju nezavisno.
 */
public record OrderCreatedEvent(
        String orderId,
        String customerEmail,
        BigDecimal total,
        List<Item> items
) {

    public record Item(String productId, int quantity, BigDecimal unitPrice) {
    }

    public static OrderCreatedEvent from(Order order) {
        return new OrderCreatedEvent(
                order.getId(),
                order.getCustomerEmail(),
                order.getTotal(),
                order.getItems().stream()
                        .map(i -> new Item(i.getProductId(), i.getQuantity(), i.getUnitPrice()))
                        .toList());
    }
}
