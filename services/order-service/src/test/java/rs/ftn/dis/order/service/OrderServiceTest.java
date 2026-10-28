package rs.ftn.dis.order.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import rs.ftn.dis.order.api.CreateOrderRequest;
import rs.ftn.dis.order.api.OrderLineRequest;
import rs.ftn.dis.order.api.OrderResponse;
import rs.ftn.dis.order.client.InventoryClient;
import rs.ftn.dis.order.client.OutOfStockException;
import rs.ftn.dis.order.client.ProductClient;
import rs.ftn.dis.order.client.ProductInfo;
import rs.ftn.dis.order.domain.Order;
import rs.ftn.dis.order.domain.OrderItem;
import rs.ftn.dis.order.domain.OrderRepository;
import rs.ftn.dis.order.domain.OrderStatus;
import rs.ftn.dis.order.events.OrderCreatedEvent;
import rs.ftn.dis.order.events.PaymentResultEvent;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit testovi toka porudzbine; drugi servisi, baza i RabbitMQ su mock objekti.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orders;

    @Mock
    private ProductClient products;

    @Mock
    private InventoryClient inventory;

    @Mock
    private StreamBridge streamBridge;

    @InjectMocks
    private OrderService service;

    private final CreateOrderRequest twoShirts = new CreateOrderRequest(
            "kupac@example.com", List.of(new OrderLineRequest("p1", 2)));

    @Test
    void createReservesStockSavesOrderAndPublishesEvent() {
        when(products.getProduct("p1")).thenReturn(new ProductInfo("p1", "Majica", new BigDecimal("1990.00")));
        when(orders.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(streamBridge.send(eq(OrderService.ORDER_CREATED_BINDING), any())).thenReturn(true);

        OrderResponse response = service.create(twoShirts);

        assertThat(response.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(response.total()).isEqualByComparingTo("3980.00");
        verify(inventory).reserve(eq(response.id()), eq(twoShirts.items()));

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(streamBridge).send(eq(OrderService.ORDER_CREATED_BINDING), event.capture());
        OrderCreatedEvent published = (OrderCreatedEvent) event.getValue();
        assertThat(published.orderId()).isEqualTo(response.id());
        assertThat(published.total()).isEqualByComparingTo("3980.00");
    }

    @Test
    void createIsRejectedWithoutSavingWhenOutOfStock() {
        when(products.getProduct("p1")).thenReturn(new ProductInfo("p1", "Majica", new BigDecimal("1990.00")));
        doThrow(new OutOfStockException("nema")).when(inventory).reserve(anyString(), anyList());

        assertThatThrownBy(() -> service.create(twoShirts)).isInstanceOf(OutOfStockException.class);

        verify(orders, never()).save(any());
        verify(streamBridge, never()).send(anyString(), any());
    }

    @Test
    void createReleasesReservationWhenEventCannotBePublished() {
        when(products.getProduct("p1")).thenReturn(new ProductInfo("p1", "Majica", new BigDecimal("1990.00")));
        when(orders.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(streamBridge.send(eq(OrderService.ORDER_CREATED_BINDING), any())).thenReturn(false);

        assertThatThrownBy(() -> service.create(twoShirts)).isInstanceOf(IllegalStateException.class);

        // kompenzacija: rezervacija se oslobadja
        verify(inventory).release(anyString());
    }

    @Test
    void completedPaymentConfirmsOrder() {
        Order order = pendingOrder();
        when(orders.findById("o1")).thenReturn(Optional.of(order));

        service.handlePaymentResult(new PaymentResultEvent("o1", "kupac@example.com", "COMPLETED", order.getTotal(), null));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void failedPaymentCancelsOrderWithReason() {
        Order order = pendingOrder();
        when(orders.findById("o1")).thenReturn(Optional.of(order));

        service.handlePaymentResult(new PaymentResultEvent("o1", "kupac@example.com", "FAILED", order.getTotal(), "Limit"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getStatusReason()).isEqualTo("Limit");
    }

    @Test
    void duplicatePaymentEventDoesNotChangeFinishedOrder() {
        Order order = pendingOrder();
        order.confirm();
        when(orders.findById("o1")).thenReturn(Optional.of(order));

        service.handlePaymentResult(new PaymentResultEvent("o1", "kupac@example.com", "FAILED", order.getTotal(), "x"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    private static Order pendingOrder() {
        return new Order("o1", "kupac@example.com",
                List.of(new OrderItem("p1", "Majica", new BigDecimal("1990.00"), 1)));
    }
}
