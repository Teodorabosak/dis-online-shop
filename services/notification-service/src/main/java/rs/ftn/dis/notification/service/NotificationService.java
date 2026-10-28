package rs.ftn.dis.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import rs.ftn.dis.notification.events.OrderCreatedEvent;
import rs.ftn.dis.notification.events.PaymentResultEvent;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Pravi tekst obavestenja i "salje" ga. Slanje je simulirano upisom u log,
 * a poslednjih 100 obavestenja se cuva u memoriji da bi se videla preko
 * GET /api/notifications. Servis namerno nema bazu: obavestenje je
 * posledica dogadjaja, ne podatak koji drugi servisi traze.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_KEPT = 100;

    private final Deque<Notification> recent = new ArrayDeque<>();

    public void onOrderCreated(OrderCreatedEvent event) {
        send(event.orderId(), event.customerEmail(),
                "Primili smo vasu porudzbinu",
                "Porudzbina " + event.orderId() + " na iznos " + event.total().toPlainString()
                        + " RSD je primljena i ceka placanje.");
    }

    public void onPaymentResult(PaymentResultEvent event) {
        if (event.isCompleted()) {
            send(event.orderId(), event.customerEmail(),
                    "Porudzbina je potvrdjena",
                    "Placanje za porudzbinu " + event.orderId() + " je uspesno. Hvala na kupovini!");
        } else {
            send(event.orderId(), event.customerEmail(),
                    "Porudzbina je otkazana",
                    "Placanje za porudzbinu " + event.orderId() + " nije uspelo: " + event.reason());
        }
    }

    public synchronized List<Notification> recent() {
        return List.copyOf(recent);
    }

    private synchronized void send(String orderId, String recipient, String subject, String message) {
        Notification notification = new Notification(Instant.now(), orderId, recipient, subject, message);
        recent.addFirst(notification);
        if (recent.size() > MAX_KEPT) {
            recent.removeLast();
        }
        log.info("[MAIL] Za: {} | Naslov: {} | {}", recipient, subject, message);
    }
}
