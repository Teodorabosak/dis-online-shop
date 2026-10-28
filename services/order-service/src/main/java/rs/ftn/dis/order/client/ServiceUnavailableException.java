package rs.ftn.dis.order.client;

/**
 * Drugi servis ne odgovara (ugasen, preopterecen, timeout) ili je circuit breaker otvoren.
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String serviceName, Throwable cause) {
        super(serviceName + " trenutno nije dostupan, pokusajte ponovo kasnije", cause);
    }
}
