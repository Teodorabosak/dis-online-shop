package rs.ftn.dis.order.events;

import java.math.BigDecimal;

/**
 * Dogadjaj koji objavljuje payment-service (exchange "payments").
 * status je "COMPLETED" ili "FAILED"; reason objasnjava neuspeh.
 */
public record PaymentResultEvent(
        String orderId,
        String customerEmail,
        String status,
        BigDecimal amount,
        String reason
) {

    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }
}
