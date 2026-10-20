package rs.ftn.dis.inventory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import rs.ftn.dis.inventory.domain.InventoryItemRepository;
import rs.ftn.dis.inventory.domain.ReservationRepository;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integracioni test: ceo servis + prava PostgreSQL baza u Docker kontejneru.
 * Flyway pri startu pravi tabele iz db/migration/V1__init.sql.
 */
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@AutoConfigureMockMvc
@Testcontainers
class InventoryApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private InventoryItemRepository items;

    @Autowired
    private ReservationRepository reservations;

    @BeforeEach
    void cleanDatabase() {
        reservations.deleteAll();
        items.deleteAll();
    }

    @Test
    void reservationReducesAvailableStock() throws Exception {
        setStock("p1", 10);

        mvc.perform(post("/api/inventory/reservations").contentType(APPLICATION_JSON)
                        .content("{\"orderId\": \"o1\", \"items\": [{\"productId\": \"p1\", \"quantity\": 3}]}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/inventory/p1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.reserved").value(3))
                .andExpect(jsonPath("$.available").value(7));
    }

    @Test
    void reservationIsAllOrNothing() throws Exception {
        setStock("p1", 10);
        setStock("p2", 1);

        // p1 ima dovoljno, p2 nema -> cela rezervacija se odbija
        mvc.perform(post("/api/inventory/reservations").contentType(APPLICATION_JSON)
                        .content("{\"orderId\": \"o2\", \"items\": ["
                                + "{\"productId\": \"p1\", \"quantity\": 2},"
                                + "{\"productId\": \"p2\", \"quantity\": 5}]}"))
                .andExpect(status().isConflict());

        // ni p1 nije ostao rezervisan: transakcija je ponistena
        mvc.perform(get("/api/inventory/p1"))
                .andExpect(jsonPath("$.reserved").value(0));
    }

    @Test
    void releasedReservationReturnsStock() throws Exception {
        setStock("p1", 5);
        mvc.perform(post("/api/inventory/reservations").contentType(APPLICATION_JSON)
                        .content("{\"orderId\": \"o3\", \"items\": [{\"productId\": \"p1\", \"quantity\": 5}]}"))
                .andExpect(status().isCreated());

        mvc.perform(delete("/api/inventory/reservations/o3"))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/inventory/p1"))
                .andExpect(jsonPath("$.available").value(5));
    }

    @Test
    void unknownProductReturns404() throws Exception {
        mvc.perform(get("/api/inventory/ne-postoji"))
                .andExpect(status().isNotFound());
    }

    @Test
    void negativeQuantityReturns400() throws Exception {
        mvc.perform(put("/api/inventory/p1").contentType(APPLICATION_JSON)
                        .content("{\"quantity\": -1}"))
                .andExpect(status().isBadRequest());
    }

    private void setStock(String productId, int quantity) throws Exception {
        mvc.perform(put("/api/inventory/" + productId).contentType(APPLICATION_JSON)
                        .content("{\"quantity\": " + quantity + "}"))
                .andExpect(status().isOk());
    }
}
