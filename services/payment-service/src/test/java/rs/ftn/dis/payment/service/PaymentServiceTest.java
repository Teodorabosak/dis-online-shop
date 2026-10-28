package rs.ftn.dis.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.ftn.dis.payment.domain.Payment;
import rs.ftn.dis.payment.domain.PaymentRepository;
import rs.ftn.dis.payment.domain.PaymentStatus;
import rs.ftn.dis.payment.events.OrderCreatedEvent;
import rs.ftn.dis.payment.events.PaymentResultEvent;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository payments;

    private PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(payments, new BigDecimal("200000"));
    }

    @Test
    void amountWithinLimitIsCompleted() {
        when(payments.findByOrderId("o1")).thenReturn(Optional.empty());

        PaymentResultEvent result = service.process(order("o1", "3980.00"));

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.reason()).isNull();
        verify(payments).save(any(Payment.class));
    }

    @Test
    void amountOverLimitFails() {
        when(payments.findByOrderId("o2")).thenReturn(Optional.empty());

        PaymentResultEvent result = service.process(order("o2", "250000.00"));

        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.reason()).contains("limit");
    }

    @Test
    void sameOrderIsNotChargedTwice() {
        Payment earlier = new Payment("o1", "kupac@example.com", new BigDecimal("3980.00"),
                PaymentStatus.COMPLETED, null);
        when(payments.findByOrderId("o1")).thenReturn(Optional.of(earlier));

        PaymentResultEvent result = service.process(order("o1", "3980.00"));

        assertThat(result.status()).isEqualTo("COMPLETED");
        verify(payments, never()).save(any());
    }

    private static OrderCreatedEvent order(String orderId, String total) {
        return new OrderCreatedEvent(orderId, "kupac@example.com", new BigDecimal(total),
                List.of(new OrderCreatedEvent.Item("p1", 1, new BigDecimal(total))));
    }
}
