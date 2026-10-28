package rs.ftn.dis.payment.events;

import rs.ftn.dis.payment.domain.Payment;

import java.math.BigDecimal;

/**
 * Rezultat placanja, objavljuje se na exchange "payments".
 * Slusaju ga order-service, inventory-service i notification-service.
 */
public record PaymentResultEvent(
        String orderId,
        String customerEmail,
        String status,
        BigDecimal amount,
        String reason
) {

    public static PaymentResultEvent from(Payment payment) {
        return new PaymentResultEvent(
                payment.getOrderId(),
                payment.getCustomerEmail(),
                payment.getStatus().name(),
                payment.getAmount(),
                payment.getReason());
    }
}
