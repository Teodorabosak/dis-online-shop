package rs.ftn.dis.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ftn.dis.order.api.CreateOrderRequest;
import rs.ftn.dis.order.api.OrderResponse;
import rs.ftn.dis.order.client.InventoryClient;
import rs.ftn.dis.order.client.ProductClient;
import rs.ftn.dis.order.client.ProductInfo;
import rs.ftn.dis.order.domain.Order;
import rs.ftn.dis.order.domain.OrderItem;
import rs.ftn.dis.order.domain.OrderRepository;
import rs.ftn.dis.order.domain.OrderStatus;
import rs.ftn.dis.order.events.OrderCreatedEvent;
import rs.ftn.dis.order.events.PaymentResultEvent;

import java.util.List;
import java.util.UUID;

/**
 * Tok porudzbine (saga):
 *
 * 1. SINHRONO: cene od product-service-a
 * 2. SINHRONO: rezervacija robe u inventory-service-u
 * 3. snimanje porudzbine (PENDING_PAYMENT)
 * 4. ASINHRONO: objava OrderCreated -> payment-service naplacuje
 * 5. ASINHRONO: stize PaymentResult -> CONFIRMED ili CANCELLED
 *
 * Nema jedne transakcije preko vise servisa (svaki ima svoju bazu). Umesto toga,
 * ako korak ne uspe, prethodni se ponistava "kompenzacijom" (npr. oslobadjanje
 * rezervacije). To je saga obrazac.
 */
@Service
public class OrderService {

    static final String ORDER_CREATED_BINDING = "orderCreated-out-0";

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orders;
    private final ProductClient products;
    private final InventoryClient inventory;
    private final StreamBridge streamBridge;

    public OrderService(OrderRepository orders, ProductClient products,
                        InventoryClient inventory, StreamBridge streamBridge) {
        this.orders = orders;
        this.products = products;
        this.inventory = inventory;
        this.streamBridge = streamBridge;
    }

    /**
     * Namerno BEZ @Transactional: pozivi drugih servisa ne smeju da drze
     * otvorenu transakciju baze dok se ceka mreza.
     */
    public OrderResponse create(CreateOrderRequest request) {
        String orderId = UUID.randomUUID().toString();

        List<OrderItem> items = request.items().stream()
                .map(line -> {
                    ProductInfo product = products.getProduct(line.productId());
                    return new OrderItem(product.id(), product.name(), product.price(), line.quantity());
                })
                .toList();

        inventory.reserve(orderId, request.items());

        try {
            Order order = orders.save(new Order(orderId, request.customerEmail(), items));
            boolean sent = streamBridge.send(ORDER_CREATED_BINDING, OrderCreatedEvent.from(order));
            if (!sent) {
                throw new IllegalStateException("Dogadjaj OrderCreated nije poslat");
            }
            log.info("Porudzbina {} napravljena, ceka placanje", orderId);
            return OrderResponse.from(order);
        } catch (RuntimeException ex) {
            // kompenzacija: roba je rezervisana, a porudzbina nije prosla do kraja
            compensateReservation(orderId, ex);
            throw ex;
        }
    }

    @Transactional
    public void handlePaymentResult(PaymentResultEvent event) {
        orders.findById(event.orderId()).ifPresentOrElse(order -> {
            if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
                // ista poruka je stigla ponovo (RabbitMQ garantuje "bar jednom") - ignorisemo
                log.info("Porudzbina {} je vec {}, poruka se ignorise", order.getId(), order.getStatus());
                return;
            }
            if (event.isCompleted()) {
                order.confirm();
            } else {
                order.cancel(event.reason());
            }
            log.info("Porudzbina {} -> {}", order.getId(), order.getStatus());
        }, () -> log.warn("Stigao rezultat placanja za nepoznatu porudzbinu {}", event.orderId()));
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(String id) {
        return orders.findById(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findByCustomer(String customerEmail) {
        return orders.findByCustomerEmailOrderByCreatedAtDesc(customerEmail).stream()
                .map(OrderResponse::from)
                .toList();
    }

    private void compensateReservation(String orderId, RuntimeException cause) {
        try {
            inventory.release(orderId);
        } catch (RuntimeException releaseFailure) {
            cause.addSuppressed(releaseFailure);
        }
    }
}
