package com.nayan.obai.inventory.repository;

import com.nayan.obai.inventory.entity.Product;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductRepositoryTest
{

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private ProductRepository productRepository;

	@Test
	@Disabled
	void simpleTestToCheckJUnitInMyLocal()
	{
		assertEquals(1,1);
	}

	@Test
	void testFindByIdCheckId()
	{
		final Product product = Product.builder()
				.quantity(123)
				.build();
		final Product savedProduct = productRepository.save(product);
		entityManager.flush();
		entityManager.clear();
		final Product actualProduct = productRepository.findById(savedProduct.getProductId()).orElseThrow();
		assertEquals(savedProduct.getProductId(), actualProduct.getProductId());
	}

	@Test
	void deductQuantityByProductId()
	{
		final Product product = Product.builder()
				.quantity(123)
				.build();
		final Product savedProduct = productRepository.save(product);
		int quantity = 10;
		entityManager.flush();

		productRepository.deductQuantityByProductId(savedProduct.getProductId(), quantity);

		// flush not required as the updating query already mentions @Modifying at the top of it which commits the transaction
		// entityManager.flush();

		// Clear persistence context to avoid Hibernate cache. Else find byId will return result form cache and not find correct results.
		entityManager.clear();

		final Product actualProduct = productRepository.findById(savedProduct.getProductId()).orElseThrow();

		int expectedQuantity = savedProduct.getQuantity() - quantity;
		assertEquals(expectedQuantity, actualProduct.getQuantity());
	}

	@Test
	@Disabled
	void checkUpdateDateUpdated() {
		final Product product = Product.builder()
				.quantity(123)
				.build();
		final Product savedProduct = productRepository.save(product);
		entityManager.flush();

		// Clear persistence context to avoid Hibernate cache. Else find byId will return result form cache and not find correct results.
		entityManager.clear();

		final Product actualProduct = productRepository.findById(savedProduct.getProductId()).orElseThrow();

		assertFalse(savedProduct.getLastUpdated().equals(actualProduct.getLastUpdated()));
	}
}