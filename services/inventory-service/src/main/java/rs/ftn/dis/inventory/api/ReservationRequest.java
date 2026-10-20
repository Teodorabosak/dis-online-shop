package rs.ftn.dis.inventory.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Zahtev order-service-a da se rezervise roba za porudzbinu, npr.
 * {"orderId": "o-1", "items": [{"productId": "p-1", "quantity": 2}]}
 */
public record ReservationRequest(
        @NotBlank String orderId,
        @NotEmpty List<@Valid ReservationLine> items
) {
}
