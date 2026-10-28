package rs.ftn.dis.notification.service;

import org.junit.jupiter.api.Test;
import rs.ftn.dis.notification.events.OrderCreatedEvent;
import rs.ftn.dis.notification.events.PaymentResultEvent;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationServiceTest {

    private final NotificationService service = new NotificationService();

    @Test
    void orderCreatedProducesReceivedNotification() {
        service.onOrderCreated(new OrderCreatedEvent("o1", "kupac@example.com", new BigDecimal("3980.00")));

        assertThat(service.recent()).singleElement().satisfies(n -> {
            assertThat(n.recipient()).isEqualTo("kupac@example.com");
            assertThat(n.subject()).isEqualTo("Primili smo vasu porudzbinu");
            assertThat(n.message()).contains("3980.00");
        });
    }

    @Test
    void failedPaymentNotificationContainsReason() {
        service.onPaymentResult(new PaymentResultEvent("o1", "kupac@example.com", "FAILED",
                new BigDecimal("250000"), "Prekoracen limit"));

        assertThat(service.recent()).singleElement().satisfies(n -> {
            assertThat(n.subject()).isEqualTo("Porudzbina je otkazana");
            assertThat(n.message()).contains("Prekoracen limit");
        });
    }

    @Test
    void newestNotificationComesFirstAndOnlyLast100AreKept() {
        for (int i = 0; i < 105; i++) {
            service.onOrderCreated(new OrderCreatedEvent("o" + i, "kupac@example.com", BigDecimal.TEN));
        }

        assertThat(service.recent()).hasSize(100);
        assertThat(service.recent().get(0).orderId()).isEqualTo("o104");
    }
}
