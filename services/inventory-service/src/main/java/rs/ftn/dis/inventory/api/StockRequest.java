package rs.ftn.dis.inventory.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Novo stanje zaliha za proizvod, npr. {"quantity": 50}. */
public record StockRequest(@NotNull @PositiveOrZero Integer quantity) {
}
