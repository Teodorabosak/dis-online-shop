package rs.ftn.dis.order.domain;

public enum OrderStatus {
    /** roba je rezervisana, ceka se rezultat placanja */
    PENDING_PAYMENT,
    /** placanje je uspelo */
    CONFIRMED,
    /** placanje nije uspelo, rezervacija se oslobadja */
    CANCELLED
}
