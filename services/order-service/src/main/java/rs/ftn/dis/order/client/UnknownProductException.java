package rs.ftn.dis.order.client;

public class UnknownProductException extends RuntimeException {

    public UnknownProductException(String productId) {
        super("Proizvod " + productId + " ne postoji u katalogu");
    }
}
