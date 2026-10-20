package rs.ftn.dis.product.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.ftn.dis.product.api.ProductRequest;
import rs.ftn.dis.product.api.ProductResponse;
import rs.ftn.dis.product.domain.Product;
import rs.ftn.dis.product.domain.ProductRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit testovi: testira se samo ProductService, a baza je zamenjena
 * "mock" objektom (lazni repozitorijum kome kazemo sta da vrati).
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private ProductService service;

    @Test
    void createSavesAllFields() {
        when(repository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId("p1");
            return saved;
        });

        ProductResponse response = service.create(new ProductRequest(
                "Majica", "Pamucna majica", "odeca", new BigDecimal("1990.00"), Map.of("velicina", "M")));

        assertThat(response.id()).isEqualTo("p1");
        assertThat(response.name()).isEqualTo("Majica");
        assertThat(response.price()).isEqualByComparingTo("1990.00");
        assertThat(response.attributes()).containsEntry("velicina", "M");
    }

    @Test
    void findByIdThrowsWhenProductDoesNotExist() {
        when(repository.findById("nema")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById("nema"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void findAllFiltersByCategoryWhenGiven() {
        Product laptop = new Product();
        laptop.setId("p2");
        laptop.setName("Laptop");
        laptop.setCategory("racunari");
        laptop.setPrice(new BigDecimal("99999"));
        when(repository.findByCategory("racunari")).thenReturn(List.of(laptop));

        List<ProductResponse> result = service.findAll("racunari");

        assertThat(result).extracting(ProductResponse::name).containsExactly("Laptop");
        verify(repository, never()).findAll();
    }

    @Test
    void deleteThrowsWhenProductDoesNotExist() {
        when(repository.existsById("nema")).thenReturn(false);

        assertThatThrownBy(() -> service.delete("nema"))
                .isInstanceOf(ProductNotFoundException.class);
        verify(repository, never()).deleteById(any());
    }
}
