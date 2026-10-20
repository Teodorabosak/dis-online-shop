package rs.ftn.dis.product.api;

import rs.ftn.dis.product.domain.Product;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Podaci koje servis vraca klijentu. Odvojeni su od entiteta (Product) da
 * promena u bazi ne bi automatski menjala javni API.
 */
public record ProductResponse(
        String id,
        String name,
        String description,
        String category,
        BigDecimal price,
        Map<String, String> attributes
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                product.getPrice(),
                Map.copyOf(product.getAttributes()));
    }
}
