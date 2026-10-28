package rs.ftn.dis.notification.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Kopija formata poruke koju objavljuje order-service.
 * Notification-u trebaju samo ova polja; stavke porudzbine Jackson ignorise.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderCreatedEvent(String orderId, String customerEmail, BigDecimal total) {
}
