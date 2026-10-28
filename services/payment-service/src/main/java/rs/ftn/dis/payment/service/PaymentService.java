package rs.ftn.dis.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ftn.dis.payment.domain.Payment;
import rs.ftn.dis.payment.domain.PaymentRepository;
import rs.ftn.dis.payment.domain.PaymentStatus;
import rs.ftn.dis.payment.events.OrderCreatedEvent;
import rs.ftn.dis.payment.events.PaymentResultEvent;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Simulirana naplata. Pravilo je namerno jednostavno i predvidivo, da bi se
 * na odbrani lako pokazala oba ishoda:
 *   iznos do limita (podrazumevano 200.000)  -> COMPLETED
 *   iznos preko limita                        -> FAILED
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository payments;
    private final BigDecimal cardLimit;

    public PaymentService(PaymentRepository payments,
                          @Value("${app.payment.card-limit:200000}") BigDecimal cardLimit) {
        this.payments = payments;
        this.cardLimit = cardLimit;
    }

    @Transactional
    public PaymentResultEvent process(OrderCreatedEvent order) {
        // Idempotentnost: RabbitMQ moze isporuciti istu poruku vise puta.
        // Porudzbina se naplacuje samo jednom, a ponovljena poruka dobija isti odgovor.
        Optional<Payment> existing = payments.findByOrderId(order.orderId());
        if (existing.isPresent()) {
            log.info("Porudzbina {} je vec obradjena, salje se isti rezultat", order.orderId());
            return PaymentResultEvent.from(existing.get());
        }

        Payment payment = charge(order);
        payments.save(payment);
        log.info("Placanje za porudzbinu {}: {}", order.orderId(), payment.getStatus());
        return PaymentResultEvent.from(payment);
    }

    @Transactional(readOnly = true)
    public Optional<Payment> findByOrderId(String orderId) {
        return payments.findByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public List<Payment> findAll() {
        return payments.findAll();
    }

    private Payment charge(OrderCreatedEvent order) {
        BigDecimal amount = order.total();
        if (amount == null || amount.signum() <= 0) {
            return new Payment(order.orderId(), order.customerEmail(), BigDecimal.ZERO,
                    PaymentStatus.FAILED, "Neispravan iznos porudzbine");
        }
        if (amount.compareTo(cardLimit) > 0) {
            return new Payment(order.orderId(), order.customerEmail(), amount,
                    PaymentStatus.FAILED, "Iznos " + amount.toPlainString()
                    + " premasuje limit kartice (" + cardLimit.toPlainString() + ")");
        }
        return new Payment(order.orderId(), order.customerEmail(), amount, PaymentStatus.COMPLETED, null);
    }
}
