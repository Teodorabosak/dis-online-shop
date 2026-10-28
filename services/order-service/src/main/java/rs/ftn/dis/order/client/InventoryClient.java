package rs.ftn.dis.order.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import rs.ftn.dis.order.api.OrderLineRequest;

import java.util.List;

/**
 * SINHRONI pozivi ka inventory-service-u: porudzbina se ne sme prihvatiti
 * dok nije sigurno da je roba rezervisana.
 */
@Component
public class InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);

    private final RestClient restClient;

    public InventoryClient(RestClient.Builder builder,
                           @Value("${app.services.inventory-url:http://inventory-service}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @CircuitBreaker(name = "inventory", fallbackMethod = "reserveFallback")
    public void reserve(String orderId, List<OrderLineRequest> lines) {
        restClient.post()
                .uri("/api/inventory/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ReservationBody(orderId, lines))
                .retrieve()
                .toBodilessEntity();
    }

    @CircuitBreaker(name = "inventory", fallbackMethod = "releaseFallback")
    public void release(String orderId) {
        restClient.delete()
                .uri("/api/inventory/reservations/{orderId}", orderId)
                .retrieve()
                .toBodilessEntity();
    }

    @SuppressWarnings("unused") // poziva je Resilience4j
    private void reserveFallback(String orderId, List<OrderLineRequest> lines, Throwable ex) {
        if (ex instanceof HttpClientErrorException.Conflict || ex instanceof HttpClientErrorException.NotFound) {
            throw new OutOfStockException("Nema dovoljno robe na stanju za ovu porudzbinu");
        }
        throw new ServiceUnavailableException("inventory-service", ex);
    }

    @SuppressWarnings("unused") // poziva je Resilience4j
    private void releaseFallback(String orderId, Throwable ex) {
        log.error("Rezervacija za porudzbinu {} nije oslobodjena: {}", orderId, ex.getMessage());
        throw new ServiceUnavailableException("inventory-service", ex);
    }

    private record ReservationBody(String orderId, List<OrderLineRequest> items) {
    }
}
