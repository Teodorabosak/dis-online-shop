package rs.ftn.dis.product.domain;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Pristup bazi. Spring Data sam generise implementaciju: save, findById,
 * findAll, deleteById... a findByCategory izvodi iz imena metode.
 */
public interface ProductRepository extends MongoRepository<Product, String> {

    List<Product> findByCategory(String category);
}
