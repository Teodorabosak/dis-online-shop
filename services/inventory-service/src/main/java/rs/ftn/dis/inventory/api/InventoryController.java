package rs.ftn.dis.inventory.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.ftn.dis.inventory.service.InventoryService;

/**
 * REST API zaliha:
 *   GET    /api/inventory/{productId}               stanje zaliha
 *   PUT    /api/inventory/{productId}               postavi kolicinu na stanju
 *   POST   /api/inventory/reservations              rezervisi robu za porudzbinu
 *   DELETE /api/inventory/reservations/{orderId}    oslobodi rezervaciju porudzbine
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping("/{productId}")
    public StockResponse getStock(@PathVariable String productId) {
        return service.getStock(productId);
    }

    @PutMapping("/{productId}")
    public StockResponse setStock(@PathVariable String productId, @Valid @RequestBody StockRequest request) {
        return service.setStock(productId, request.quantity());
    }

    @PostMapping("/reservations")
    public ResponseEntity<ReservationResponse> reserve(@Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.reserve(request));
    }

    @DeleteMapping("/reservations/{orderId}")
    public ResponseEntity<Void> release(@PathVariable String orderId) {
        service.release(orderId);
        return ResponseEntity.noContent().build();
    }
}
