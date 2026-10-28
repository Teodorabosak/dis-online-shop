package rs.ftn.dis.inventory.events;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rs.ftn.dis.inventory.service.InventoryService;

import java.util.function.Consumer;

/**
 * ASINHRONO zavrsavanje rezervacije (red "payments.inventory-service"):
 *   placanje uspelo   -> roba se trajno skida sa stanja
 *   placanje palo     -> rezervacija se oslobadja (kompenzacija u sagi)
 */
@Configuration
public class PaymentEventsConfig {

    @Bean
    public Consumer<PaymentResultEvent> paymentResult(InventoryService inventory) {
        return event -> {
            if (event.isCompleted()) {
                inventory.commit(event.orderId());
            } else {
                inventory.release(event.orderId());
            }
        };
    }
}
