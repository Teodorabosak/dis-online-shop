package rs.ftn.dis.inventory.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByOrderId(String orderId);

    List<Reservation> findByOrderIdAndStatus(String orderId, ReservationStatus status);
}
