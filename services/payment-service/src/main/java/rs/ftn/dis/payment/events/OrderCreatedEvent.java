package rs.ftn.dis.payment.events;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dogadjaj koji objavljuje order-service (exchange "orders").
 * Ovo je kopija formata poruke, ne deljeni kod: servisi se dogovaraju samo o JSON-u.
 */
public record OrderCreatedEvent(
        String orderId,
        String customerEmail,
        BigDecimal total,
        List<Item> items
) {

    public record Item(String productId, int quantity, BigDecimal unitPrice) {
    }
}
