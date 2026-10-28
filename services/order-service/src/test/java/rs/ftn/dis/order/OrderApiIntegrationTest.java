package rs.ftn.dis.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import rs.ftn.dis.order.client.InventoryClient;
import rs.ftn.dis.order.client.OutOfStockException;
import rs.ftn.dis.order.client.ProductClient;
import rs.ftn.dis.order.client.ProductInfo;
import rs.ftn.dis.order.client.ServiceUnavailableException;
import rs.ftn.dis.order.client.UnknownProductException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integracioni test order-service-a sa pravom PostgreSQL bazom i pravim RabbitMQ brokerom.
 * product-service i inventory-service su zamenjeni mock objektima (@MockBean),
 * jer test proverava ovaj servis, a ne ceo sistem.
 */
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@AutoConfigureMockMvc
@Testcontainers
class OrderApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-alpine");

    private static final String ORDER_REQUEST = """
            {"customerEmail": "kupac@example.com", "items": [{"productId": "p1", "quantity": 2}]}
            """;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ProductClient productClient;

    @MockBean
    private InventoryClient inventoryClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CachingConnectionFactory connectionFactory;
    private RabbitTemplate rabbitTemplate;
    private Queue ordersSpy;

    @BeforeEach
    void setUp() {
        when(productClient.getProduct("p1")).thenReturn(new ProductInfo("p1", "Majica", new BigDecimal("1990.00")));

        // test "prisluskuje" exchange "orders" da proveri da je dogadjaj objavljen
        connectionFactory = new CachingConnectionFactory(rabbit.getHost(), rabbit.getAmqpPort());
        connectionFactory.setUsername(rabbit.getAdminUsername());
        connectionFactory.setPassword(rabbit.getAdminPassword());
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        TopicExchange orders = new TopicExchange("orders");
        admin.declareExchange(orders);
        ordersSpy = new AnonymousQueue();
        admin.declareQueue(ordersSpy);
        admin.declareBinding(BindingBuilder.bind(ordersSpy).to(orders).with("#"));
        rabbitTemplate = new RabbitTemplate(connectionFactory);
    }

    @AfterEach
    void tearDown() {
        connectionFactory.destroy();
    }

    @Test
    void createdOrderIsPublishedAndConfirmedWhenPaymentCompletes() throws Exception {
        String response = mvc.perform(post("/api/orders").contentType(APPLICATION_JSON).content(ORDER_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andReturn().getResponse().getContentAsString();
        String orderId = JsonPath.read(response, "$.id");

        // 1) OrderCreated je objavljen na RabbitMQ
        Message message = rabbitTemplate.receive(ordersSpy.getName(), 10_000);
        assertThat(message).as("OrderCreated poruka").isNotNull();
        JsonNode event = objectMapper.readTree(message.getBody());
        assertThat(event.get("orderId").asText()).isEqualTo(orderId);
        assertThat(event.get("total").decimalValue()).isEqualByComparingTo("3980.00");

        // 2) payment-service (ovde: test) javlja da je placanje uspelo
        publishPaymentResult("""
                {"orderId": "%s", "customerEmail": "kupac@example.com", "status": "COMPLETED", "amount": 3980.00}
                """.formatted(orderId));

        // 3) porudzbina asinhrono prelazi u CONFIRMED
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                mvc.perform(get("/api/orders/" + orderId))
                        .andExpect(jsonPath("$.status").value("CONFIRMED")));
    }

    @Test
    void failedPaymentCancelsOrder() throws Exception {
        String response = mvc.perform(post("/api/orders").contentType(APPLICATION_JSON).content(ORDER_REQUEST))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String orderId = JsonPath.read(response, "$.id");

        publishPaymentResult("""
                {"orderId": "%s", "customerEmail": "kupac@example.com", "status": "FAILED",
                 "amount": 3980.00, "reason": "Prekoracen limit"}
                """.formatted(orderId));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                mvc.perform(get("/api/orders/" + orderId))
                        .andExpect(jsonPath("$.status").value("CANCELLED"))
                        .andExpect(jsonPath("$.statusReason").value("Prekoracen limit")));
    }

    @Test
    void outOfStockReturns409() throws Exception {
        doThrow(new OutOfStockException("Nema robe")).when(inventoryClient).reserve(anyString(), anyList());

        mvc.perform(post("/api/orders").contentType(APPLICATION_JSON).content(ORDER_REQUEST))
                .andExpect(status().isConflict());
    }

    @Test
    void unknownProductReturns422() throws Exception {
        when(productClient.getProduct("p1")).thenThrow(new UnknownProductException("p1"));

        mvc.perform(post("/api/orders").contentType(APPLICATION_JSON).content(ORDER_REQUEST))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void unavailableInventoryReturns503() throws Exception {
        doThrow(new ServiceUnavailableException("inventory-service", new RuntimeException("down")))
                .when(inventoryClient).reserve(anyString(), anyList());

        mvc.perform(post("/api/orders").contentType(APPLICATION_JSON).content(ORDER_REQUEST))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void invalidOrderReturns400() throws Exception {
        mvc.perform(post("/api/orders").contentType(APPLICATION_JSON)
                        .content("{\"customerEmail\": \"nije-mejl\", \"items\": []}"))
                .andExpect(status().isBadRequest());
    }

    private void publishPaymentResult(String json) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType("application/json");
        rabbitTemplate.send("payments", "payments", new Message(json.getBytes(StandardCharsets.UTF_8), properties));
    }
}
