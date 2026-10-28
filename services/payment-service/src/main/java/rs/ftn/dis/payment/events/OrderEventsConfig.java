package rs.ftn.dis.payment.events;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rs.ftn.dis.payment.service.PaymentService;

import java.util.function.Function;

/**
 * Function = primi poruku, vrati poruku. Spring Cloud Stream:
 *   - cita OrderCreated iz reda "orders.payment-service"  (binding processOrder-in-0)
 *   - povratnu vrednost salje na exchange "payments"       (binding processOrder-out-0)
 */
@Configuration
public class OrderEventsConfig {

    @Bean
    public Function<OrderCreatedEvent, PaymentResultEvent> processOrder(PaymentService paymentService) {
        return paymentService::process;
    }
}
