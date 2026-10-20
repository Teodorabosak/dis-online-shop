package rs.ftn.dis.inventory.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ftn.dis.inventory.api.ReservationLine;
import rs.ftn.dis.inventory.api.ReservationRequest;
import rs.ftn.dis.inventory.api.ReservationResponse;
import rs.ftn.dis.inventory.api.StockResponse;
import rs.ftn.dis.inventory.domain.InventoryItem;
import rs.ftn.dis.inventory.domain.InventoryItemRepository;
import rs.ftn.dis.inventory.domain.Reservation;
import rs.ftn.dis.inventory.domain.ReservationRepository;
import rs.ftn.dis.inventory.domain.ReservationStatus;

import java.util.List;

/**
 * Poslovna logika zaliha.
 *
 * Kljucna stvar je @Transactional na reserve(): rezervacija vise proizvoda je
 * "sve ili nista". Ako za bilo koju stavku nema dovoljno robe, baca se izuzetak
 * i baza ponistava (rollback) i rezervacije stavki koje su vec prosle.
 */
@Service
public class InventoryService {

    private final InventoryItemRepository items;
    private final ReservationRepository reservations;

    public InventoryService(InventoryItemRepository items, ReservationRepository reservations) {
        this.items = items;
        this.reservations = reservations;
    }

    @Transactional(readOnly = true)
    public StockResponse getStock(String productId) {
        return StockResponse.from(getItemOrThrow(productId));
    }

    @Transactional
    public StockResponse setStock(String productId, int quantity) {
        InventoryItem item = items.findByProductId(productId)
                .orElseGet(() -> new InventoryItem(productId, 0));
        item.changeQuantity(quantity);
        return StockResponse.from(items.save(item));
    }

    @Transactional
    public ReservationResponse reserve(ReservationRequest request) {
        // Idempotentnost: ako je ista porudzbina vec rezervisana (npr. order-service
        // je ponovio zahtev posle timeout-a), ne rezervisemo robu dva puta.
        List<Reservation> existing = reservations.findByOrderId(request.orderId());
        if (!existing.isEmpty()) {
            return toResponse(request.orderId(), existing);
        }

        for (ReservationLine line : request.items()) {
            InventoryItem item = getItemOrThrow(line.productId());
            item.reserve(line.quantity()); // baca InsufficientStockException -> rollback svega
        }

        List<Reservation> saved = reservations.saveAll(request.items().stream()
                .map(line -> new Reservation(request.orderId(), line.productId(), line.quantity()))
                .toList());
        return toResponse(request.orderId(), saved);
    }

    @Transactional
    public void release(String orderId) {
        List<Reservation> active = reservations.findByOrderIdAndStatus(orderId, ReservationStatus.ACTIVE);
        for (Reservation reservation : active) {
            items.findByProductId(reservation.getProductId())
                    .ifPresent(item -> item.release(reservation.getQuantity()));
            reservation.markReleased();
        }
    }

    private InventoryItem getItemOrThrow(String productId) {
        return items.findByProductId(productId).orElseThrow(() -> new StockNotFoundException(productId));
    }

    private static ReservationResponse toResponse(String orderId, List<Reservation> list) {
        return new ReservationResponse(orderId, list.stream()
                .map(r -> new ReservationLine(r.getProductId(), r.getQuantity()))
                .toList());
    }
}
