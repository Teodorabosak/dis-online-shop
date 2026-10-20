package rs.ftn.dis.product.service;

import org.springframework.stereotype.Service;
import rs.ftn.dis.product.api.ProductRequest;
import rs.ftn.dis.product.api.ProductResponse;
import rs.ftn.dis.product.domain.Product;
import rs.ftn.dis.product.domain.ProductRepository;

import java.util.List;

/**
 * Poslovna logika kataloga. Kontroler (HTTP) poziva ovu klasu,
 * a ona koristi repozitorijum (bazu).
 */
@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<ProductResponse> findAll(String category) {
        List<Product> products = (category == null || category.isBlank())
                ? repository.findAll()
                : repository.findByCategory(category);
        return products.stream().map(ProductResponse::from).toList();
    }

    public ProductResponse findById(String id) {
        return ProductResponse.from(getOrThrow(id));
    }

    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        apply(product, request);
        return ProductResponse.from(repository.save(product));
    }

    public ProductResponse update(String id, ProductRequest request) {
        Product product = getOrThrow(id);
        apply(product, request);
        return ProductResponse.from(repository.save(product));
    }

    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        repository.deleteById(id);
    }

    private Product getOrThrow(String id) {
        return repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    private static void apply(Product product, ProductRequest request) {
        product.setName(request.name());
        product.setDescription(request.description());
        product.setCategory(request.category());
        product.setPrice(request.price());
        product.setAttributes(request.attributes());
    }
}
