package rs.ftn.dis.notification.events;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rs.ftn.dis.notification.service.NotificationService;

import java.util.function.Consumer;

/**
 * Notification slusa DVA toka dogadjaja, svaki preko svog reda:
 *   orders.notification-service    <- OrderCreated
 *   payments.notification-service  <- PaymentResult
 */
@Configuration
public class EventsConfig {

    @Bean
    public Consumer<OrderCreatedEvent> orderCreated(NotificationService notifications) {
        return notifications::onOrderCreated;
    }

    @Bean
    public Consumer<PaymentResultEvent> paymentResult(NotificationService notifications) {
        return notifications::onPaymentResult;
    }
}
