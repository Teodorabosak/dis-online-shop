package rs.ftn.dis.notification;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Integracioni test: test objavi dogadjaje na pravi RabbitMQ, a notification-service
 * treba da ih primi i napravi obavestenja.
 */
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@AutoConfigureMockMvc
@Testcontainers
class NotificationFlowIntegrationTest {

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-alpine");

    @Autowired
    private MockMvc mvc;

    private CachingConnectionFactory connectionFactory;
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        connectionFactory = new CachingConnectionFactory(rabbit.getHost(), rabbit.getAmqpPort());
        connectionFactory.setUsername(rabbit.getAdminUsername());
        connectionFactory.setPassword(rabbit.getAdminPassword());
        rabbitTemplate = new RabbitTemplate(connectionFactory);
    }

    @AfterEach
    void tearDown() {
        connectionFactory.destroy();
    }

    @Test
    void orderAndPaymentEventsBecomeNotifications() {
        publish("orders", """
                {"orderId": "o-77", "customerEmail": "kupac@example.com", "total": 3980.00, "items": []}
                """);
        publish("payments", """
                {"orderId": "o-77", "customerEmail": "kupac@example.com", "status": "COMPLETED", "amount": 3980.00}
                """);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                mvc.perform(get("/api/notifications"))
                        .andExpect(jsonPath("$[*].subject",
                                hasItems("Primili smo vasu porudzbinu", "Porudzbina je potvrdjena"))));
    }

    private void publish(String exchange, String json) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType("application/json");
        rabbitTemplate.send(exchange, exchange, new Message(json.getBytes(StandardCharsets.UTF_8), properties));
    }
}
