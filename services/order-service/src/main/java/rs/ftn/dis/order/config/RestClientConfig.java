package rs.ftn.dis.order.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * HTTP klijent za sinhrone pozive ka drugim servisima.
 *
 * @LoadBalanced znaci da adresa "http://product-service" nije prava adresa,
 * vec ime servisa: Spring Cloud LoadBalancer pita Eureku gde se servis nalazi
 * i bira jednu od pokrenutih instanci.
 */
@Configuration
public class RestClientConfig {

    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        // Kratki timeout-i: bolje brzo odustati nego da porudzbina visi
        // dok drugi servis ne odgovara (circuit breaker onda preuzima).
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        return RestClient.builder().requestFactory(requestFactory);
    }
}
