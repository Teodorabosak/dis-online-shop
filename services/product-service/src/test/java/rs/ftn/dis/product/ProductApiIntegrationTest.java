package rs.ftn.dis.product;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import rs.ftn.dis.product.domain.ProductRepository;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integracioni test: pokrece CEO servis i pravu MongoDB bazu u Docker kontejneru
 * (Testcontainers), pa salje HTTP zahteve kao pravi klijent.
 * Config Server i Eureka su iskljuceni jer test ne treba ostatak sistema.
 */
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@AutoConfigureMockMvc
@Testcontainers
class ProductApiIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ProductRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void createdProductCanBeFetchedById() throws Exception {
        String body = """
                {"name": "Majica", "description": "Pamucna", "category": "odeca",
                 "price": 1990.00, "attributes": {"velicina": "M", "boja": "plava"}}
                """;

        String response = mvc.perform(post("/api/products").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(response, "$.id");

        mvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Majica"))
                .andExpect(jsonPath("$.attributes.velicina").value("M"));
    }

    @Test
    void productsCanBeFilteredByCategory() throws Exception {
        mvc.perform(post("/api/products").contentType(APPLICATION_JSON)
                .content("{\"name\": \"Majica\", \"category\": \"odeca\", \"price\": 1990}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/products").contentType(APPLICATION_JSON)
                .content("{\"name\": \"Laptop\", \"category\": \"racunari\", \"price\": 99999}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/products").param("category", "racunari"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Laptop"));
    }

    @Test
    void unknownProductReturns404() throws Exception {
        mvc.perform(get("/api/products/ne-postoji"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/products/ne-postoji"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidProductReturns400() throws Exception {
        mvc.perform(post("/api/products").contentType(APPLICATION_JSON)
                .content("{\"name\": \"\", \"category\": \"odeca\", \"price\": -5}"))
                .andExpect(status().isBadRequest());
    }
}
