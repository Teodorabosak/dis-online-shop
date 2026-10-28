package rs.ftn.dis.order.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Porudzbina kupca. Tabela se zove "orders" jer je ORDER rezervisana rec u SQL-u.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    private String id;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(name = "status_reason")
    private String statusReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();

    @Version
    private Long version;

    protected Order() {
        // potreban JPA-u
    }

    public Order(String id, String customerEmail, List<OrderItem> items) {
        this.id = id;
        this.customerEmail = customerEmail;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.createdAt = Instant.now();
        items.forEach(this::addItem);
        this.total = this.items.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    public void confirm() {
        this.status = OrderStatus.CONFIRMED;
        this.statusReason = null;
    }

    public void cancel(String reason) {
        this.status = OrderStatus.CANCELLED;
        this.statusReason = reason;
    }

    public String getId() {
        return id;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }
}
