package rs.ftn.dis.inventory.service;

public class StockNotFoundException extends RuntimeException {

    public StockNotFoundException(String productId) {
        super("Za proizvod " + productId + " ne postoji evidencija zaliha");
    }
}
