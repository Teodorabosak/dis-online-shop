package rs.ftn.dis.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integracioni test: test objavi OrderCreated na RabbitMQ (kao da je to uradio
 * order-service) i ceka da payment-service objavi PaymentResult.
 */
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@AutoConfigureMockMvc
@Testcontainers
class PaymentFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-alpine");

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CachingConnectionFactory connectionFactory;
    private RabbitTemplate rabbitTemplate;
    private Queue paymentsSpy;

    @BeforeEach
    void setUp() {
        connectionFactory = new CachingConnectionFactory(rabbit.getHost(), rabbit.getAmqpPort());
        connectionFactory.setUsername(rabbit.getAdminUsername());
        connectionFactory.setPassword(rabbit.getAdminPassword());
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        TopicExchange payments = new TopicExchange("payments");
        admin.declareExchange(payments);
        paymentsSpy = new AnonymousQueue();
        admin.declareQueue(paymentsSpy);
        admin.declareBinding(BindingBuilder.bind(paymentsSpy).to(payments).with("#"));
        rabbitTemplate = new RabbitTemplate(connectionFactory);
    }

    @AfterEach
    void tearDown() {
        connectionFactory.destroy();
    }

    @Test
    void orderWithinLimitIsPaid() throws Exception {
        String orderId = UUID.randomUUID().toString();

        JsonNode result = publishOrderAndAwaitResult(orderId, "3980.00");

        assertThat(result.get("status").asText()).isEqualTo("COMPLETED");
        mvc.perform(get("/api/payments/order/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void orderOverLimitIsRejected() throws Exception {
        String orderId = UUID.randomUUID().toString();

        JsonNode result = publishOrderAndAwaitResult(orderId, "250000.00");

        assertThat(result.get("status").asText()).isEqualTo("FAILED");
        assertThat(result.get("reason").asText()).contains("limit");
    }

    @Test
    void unknownPaymentReturns404() throws Exception {
        mvc.perform(get("/api/payments/order/ne-postoji"))
                .andExpect(status().isNotFound());
    }

    private JsonNode publishOrderAndAwaitResult(String orderId, String total) throws Exception {
        String json = """
                {"orderId": "%s", "customerEmail": "kupac@example.com", "total": %s,
                 "items": [{"productId": "p1", "quantity": 1, "unitPrice": %s}]}
                """.formatted(orderId, total, total);
        MessageProperties properties = new MessageProperties();
        properties.setContentType("application/json");
        rabbitTemplate.send("orders", "orders", new Message(json.getBytes(StandardCharsets.UTF_8), properties));

        Message message = rabbitTemplate.receive(paymentsSpy.getName(), 15_000);
        assertThat(message).as("PaymentResult poruka").isNotNull();
        JsonNode result = objectMapper.readTree(message.getBody());
        assertThat(result.get("orderId").asText()).isEqualTo(orderId);
        return result;
    }
}
