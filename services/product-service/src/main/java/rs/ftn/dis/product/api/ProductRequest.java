package rs.ftn.dis.product.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Podaci koje klijent salje kad pravi ili menja proizvod (JSON telo zahteva).
 * Anotacije proveravaju ispravnost; neispravan zahtev dobija 400 Bad Request.
 */
public record ProductRequest(
        @NotBlank String name,
        String description,
        @NotBlank String category,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        Map<String, String> attributes
) {
}
