package rs.ftn.dis.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway: jedina ulazna tacka u sistem spolja. Prosledjuje zahteve
 * odgovarajucem servisu (rute su u config-repo/gateway.yml), a kasnije ce
 * ovde biti i provera OAuth2 tokena.
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
