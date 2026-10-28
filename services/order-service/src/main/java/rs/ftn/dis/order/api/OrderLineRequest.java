package rs.ftn.dis.order.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record OrderLineRequest(@NotBlank String productId, @Positive int quantity) {
}
