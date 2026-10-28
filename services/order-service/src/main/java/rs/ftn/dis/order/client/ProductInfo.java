package rs.ftn.dis.order.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Samo ona polja iz odgovora product-service-a koja order-service koristi.
 * Ostala polja (opis, atributi...) Jackson ignorise.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductInfo(String id, String name, BigDecimal price) {
}
