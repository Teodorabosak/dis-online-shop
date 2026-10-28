package rs.ftn.dis.inventory.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Kopija formata poruke koju objavljuje payment-service (exchange "payments"). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentResultEvent(String orderId, String status) {

    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }
}
