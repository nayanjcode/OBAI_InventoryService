package com.nayan.obai.inventory.repository;

import com.nayan.obai.inventory.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>
{
	@Modifying
	@Query(value = "UPDATE Product SET quantity = :quantity WHERE productId = :productId", nativeQuery = true)
	void updateQuantityByProductId(@Param("productId") UUID productId, @Param("quantity") int quantity);

}
