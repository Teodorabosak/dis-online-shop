package rs.ftn.dis.order.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * SINHRONI poziv ka product-service-u: order-service mora da zna tacnu cenu
 * proizvoda pre nego sto napravi porudzbinu, pa ceka odgovor.
 *
 * @CircuitBreaker: ako product-service vise puta zaredom ne odgovori, "osigurac"
 * se otvori i sledeci pozivi odmah zavrsavaju u fallback metodi (bez cekanja
 * na timeout). Posle 10 sekundi pusta nekoliko probnih poziva da proveri da li
 * se servis oporavio.
 */
@Component
public class ProductClient {

    private final RestClient restClient;

    public ProductClient(RestClient.Builder builder,
                         @Value("${app.services.product-url:http://product-service}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @CircuitBreaker(name = "product", fallbackMethod = "getProductFallback")
    public ProductInfo getProduct(String productId) {
        return restClient.get()
                .uri("/api/products/{id}", productId)
                .retrieve()
                .body(ProductInfo.class);
    }

    @SuppressWarnings("unused") // poziva je Resilience4j
    private ProductInfo getProductFallback(String productId, Throwable ex) {
        if (ex instanceof HttpClientErrorException.NotFound) {
            throw new UnknownProductException(productId);
        }
        throw new ServiceUnavailableException("product-service", ex);
    }
}
