package rs.ftn.dis.inventory.domain;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String productId, int requested, int available) {
        super("Nema dovoljno robe za proizvod " + productId
                + ": trazeno " + requested + ", dostupno " + available);
    }
}
