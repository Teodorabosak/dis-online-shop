package rs.ftn.dis.order.service;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String id) {
        super("Porudzbina " + id + " ne postoji");
    }
}
