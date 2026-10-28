package rs.ftn.dis.notification.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.ftn.dis.notification.service.Notification;
import rs.ftn.dis.notification.service.NotificationService;

import java.util.List;

/** GET /api/notifications - poslednja poslata obavestenja, najnovija prva. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public List<Notification> recent() {
        return service.recent();
    }
}
