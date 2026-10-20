package rs.ftn.dis.inventory.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.ftn.dis.inventory.api.ReservationLine;
import rs.ftn.dis.inventory.api.ReservationRequest;
import rs.ftn.dis.inventory.api.ReservationResponse;
import rs.ftn.dis.inventory.domain.InsufficientStockException;
import rs.ftn.dis.inventory.domain.InventoryItem;
import rs.ftn.dis.inventory.domain.InventoryItemRepository;
import rs.ftn.dis.inventory.domain.Reservation;
import rs.ftn.dis.inventory.domain.ReservationRepository;
import rs.ftn.dis.inventory.domain.ReservationStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit testovi poslovne logike zaliha; baza je zamenjena mock objektima.
 */
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryItemRepository items;

    @Mock
    private ReservationRepository reservations;

    @InjectMocks
    private InventoryService service;

    @Test
    void reserveDecreasesAvailableStock() {
        InventoryItem majica = new InventoryItem("p1", 10);
        when(reservations.findByOrderId("o1")).thenReturn(List.of());
        when(items.findByProductId("p1")).thenReturn(Optional.of(majica));
        when(reservations.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponse response = service.reserve(
                new ReservationRequest("o1", List.of(new ReservationLine("p1", 3))));

        assertThat(majica.getReserved()).isEqualTo(3);
        assertThat(majica.getAvailable()).isEqualTo(7);
        assertThat(response.items()).containsExactly(new ReservationLine("p1", 3));
    }

    @Test
    void reserveFailsWhenNotEnoughStock() {
        InventoryItem majica = new InventoryItem("p1", 2);
        when(reservations.findByOrderId("o1")).thenReturn(List.of());
        when(items.findByProductId("p1")).thenReturn(Optional.of(majica));

        assertThatThrownBy(() -> service.reserve(
                new ReservationRequest("o1", List.of(new ReservationLine("p1", 5)))))
                .isInstanceOf(InsufficientStockException.class);
        verify(reservations, never()).saveAll(anyList());
    }

    @Test
    void reserveIsIdempotentForTheSameOrder() {
        when(reservations.findByOrderId("o1")).thenReturn(List.of(new Reservation("o1", "p1", 3)));

        ReservationResponse response = service.reserve(
                new ReservationRequest("o1", List.of(new ReservationLine("p1", 3))));

        assertThat(response.items()).containsExactly(new ReservationLine("p1", 3));
        verify(items, never()).findByProductId("p1");
    }

    @Test
    void releaseReturnsReservedStock() {
        InventoryItem majica = new InventoryItem("p1", 10);
        majica.reserve(4);
        Reservation reservation = new Reservation("o1", "p1", 4);
        when(reservations.findByOrderIdAndStatus("o1", ReservationStatus.ACTIVE)).thenReturn(List.of(reservation));
        when(items.findByProductId("p1")).thenReturn(Optional.of(majica));

        service.release("o1");

        assertThat(majica.getAvailable()).isEqualTo(10);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    void stockCannotBeSetBelowReservedQuantity() {
        InventoryItem majica = new InventoryItem("p1", 10);
        majica.reserve(6);
        when(items.findByProductId("p1")).thenReturn(Optional.of(majica));

        assertThatThrownBy(() -> service.setStock("p1", 5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
