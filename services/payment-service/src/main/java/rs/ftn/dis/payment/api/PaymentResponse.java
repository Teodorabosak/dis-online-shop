package rs.ftn.dis.payment.api;

import rs.ftn.dis.payment.domain.Payment;
import rs.ftn.dis.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        String orderId,
        String customerEmail,
        BigDecimal amount,
        PaymentStatus status,
        String reason,
        Instant createdAt
) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getOrderId(),
                payment.getCustomerEmail(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getReason(),
                payment.getCreatedAt());
    }
}
