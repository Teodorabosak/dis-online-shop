package rs.ftn.dis.inventory.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** Jedna stavka rezervacije: koji proizvod i koliko komada. */
public record ReservationLine(@NotBlank String productId, @Positive int quantity) {
}
