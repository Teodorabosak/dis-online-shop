package rs.ftn.dis.product.service;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String id) {
        super("Proizvod sa id-jem " + id + " ne postoji");
    }
}
