package rs.ftn.dis.order.api;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import rs.ftn.dis.order.service.OrderService;

import java.net.URI;
import java.util.List;

/**
 * REST API porudzbina:
 *   POST /api/orders                      nova porudzbina (odgovor: 201, status PENDING_PAYMENT)
 *   GET  /api/orders/{id}                 jedna porudzbina (status se menja kad stigne placanje)
 *   GET  /api/orders?customerEmail=...    porudzbine kupca
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public OrderResponse findById(@PathVariable String id) {
        return service.findById(id);
    }

    @GetMapping
    public List<OrderResponse> findByCustomer(@RequestParam String customerEmail) {
        return service.findByCustomer(customerEmail);
    }
}
