package rs.ftn.dis.product.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Proizvod iz kataloga, snima se kao dokument u MongoDB kolekciju "products".
 * Polje "attributes" je razlog za MongoDB: razlicite vrste proizvoda imaju
 * razlicite atribute (majica: velicina, boja; laptop: RAM, procesor).
 */
@Document(collection = "products")
public class Product {

    @Id
    private String id;
    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private Map<String, String> attributes = new HashMap<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes == null ? new HashMap<>() : new HashMap<>(attributes);
    }
}
