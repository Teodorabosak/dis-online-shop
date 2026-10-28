package rs.ftn.dis.order.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Nova porudzbina, npr.
 * {"customerEmail": "kupac@example.com", "items": [{"productId": "abc", "quantity": 2}]}
 */
public record CreateOrderRequest(
        @NotBlank @Email String customerEmail,
        @NotEmpty List<@Valid OrderLineRequest> items
) {
}
