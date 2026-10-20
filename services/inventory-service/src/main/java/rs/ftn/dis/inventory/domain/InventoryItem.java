package rs.ftn.dis.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Stanje zaliha jednog proizvoda (red u tabeli inventory_items).
 *
 * quantity  = koliko komada fizicki ima u magacinu
 * reserved  = koliko je od toga vec obecano porudzbinama koje jos nisu placene
 * available = quantity - reserved = koliko jos moze da se proda
 */
@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, unique = true)
    private String productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int reserved;

    /**
     * Optimisticko zakljucavanje: ako dve porudzbine istovremeno menjaju isti red,
     * druga dobija gresku umesto da tiho pregazi izmenu prve.
     */
    @Version
    private long version;

    protected InventoryItem() {
        // potreban JPA-u
    }

    public InventoryItem(String productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public int getAvailable() {
        return quantity - reserved;
    }

    public void reserve(int amount) {
        if (amount > getAvailable()) {
            throw new InsufficientStockException(productId, amount, getAvailable());
        }
        reserved += amount;
    }

    public void release(int amount) {
        reserved = Math.max(0, reserved - amount);
    }

    public void changeQuantity(int newQuantity) {
        if (newQuantity < reserved) {
            throw new IllegalArgumentException(
                    "Kolicina ne moze biti manja od rezervisane (" + reserved + ")");
        }
        this.quantity = newQuantity;
    }

    public Long getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getReserved() {
        return reserved;
    }
}
