package rs.ftn.dis.notification.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** Kopija formata poruke koju objavljuje payment-service. */
@JsonIgnoreProperties(ignoreUnknown = true)
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
