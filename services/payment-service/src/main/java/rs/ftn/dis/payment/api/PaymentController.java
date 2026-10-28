package rs.ftn.dis.payment.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.ftn.dis.payment.service.PaymentService;

import java.util.List;

/**
 * Samo citanje: placanja ne pokrece klijent, vec dogadjaj OrderCreated.
 *   GET /api/payments                    sva placanja
 *   GET /api/payments/order/{orderId}    placanje jedne porudzbine
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @GetMapping
    public List<PaymentResponse> findAll() {
        return service.findAll().stream().map(PaymentResponse::from).toList();
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> findByOrderId(@PathVariable String orderId) {
        return service.findByOrderId(orderId)
                .map(PaymentResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
