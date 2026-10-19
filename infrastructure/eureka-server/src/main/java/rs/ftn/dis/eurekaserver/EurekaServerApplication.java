package rs.ftn.dis.eurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Service discovery: svaki servis se pri startu registruje ovde (ime + adresa),
 * a ostali ga pronalaze po imenu (npr. "product-service") umesto po fiksnoj IP adresi.
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
