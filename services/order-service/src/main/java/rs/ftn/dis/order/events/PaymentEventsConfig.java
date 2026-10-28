package rs.ftn.dis.order.events;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rs.ftn.dis.order.service.OrderService;

import java.util.function.Consumer;

/**
 * ASINHRONI prijem poruka. Spring Cloud Stream povezuje ovaj Consumer sa
 * RabbitMQ redom "payments.order-service" (podesavanje u application.yml,
 * binding "paymentResult-in-0"). Svaka poruka o placanju poziva handlePaymentResult.
 */
@Configuration
public class PaymentEventsConfig {

    @Bean
    public Consumer<PaymentResultEvent> paymentResult(OrderService orderService) {
        return orderService::handlePaymentResult;
    }
}
