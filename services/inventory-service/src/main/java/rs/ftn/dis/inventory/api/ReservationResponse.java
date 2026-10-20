package rs.ftn.dis.inventory.api;

import java.util.List;

public record ReservationResponse(String orderId, List<ReservationLine> items) {
}
