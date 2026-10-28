package rs.ftn.dis.order.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Upisuje u log svaku promenu stanja circuit breaker-a (CLOSED -> OPEN -> HALF_OPEN...),
 * da bi se na demonstraciji videlo kad se "osigurac" otvorio i zatvorio:
 *   docker compose logs -f order-service
 */
@Configuration
public class CircuitBreakerLoggingConfig {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerLoggingConfig.class);

    public CircuitBreakerLoggingConfig(CircuitBreakerRegistry registry) {
        registry.getAllCircuitBreakers().forEach(this::logTransitions);
        registry.getEventPublisher().onEntryAdded(event -> logTransitions(event.getAddedEntry()));
    }

    private void logTransitions(CircuitBreaker circuitBreaker) {
        circuitBreaker.getEventPublisher().onStateTransition(event ->
                log.warn("Circuit breaker '{}' promenio stanje: {}",
                        event.getCircuitBreakerName(), event.getStateTransition()));
    }
}
