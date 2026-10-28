package rs.ftn.dis.notification.service;

import java.time.Instant;

/** Jedno poslato obavestenje (u ovoj simulaciji: zapis u logu umesto pravog mejla). */
public record Notification(Instant sentAt, String orderId, String recipient, String subject, String message) {
}
