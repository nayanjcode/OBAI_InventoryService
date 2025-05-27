package com.nayan.obai.inventory.repository;

import com.nayan.obai.inventory.entity.ProductOrderReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<ProductOrderReservation, UUID>
{
	void deleteByOrderId(UUID orderId);

	@Query("SELECT COALESCE(SUM(r.reservedQuantity), 0) FROM ProductOrderReservation r WHERE r.productId = :productId")
	int getTotalReservedQuantityByProductId(@Param("productId") UUID productId);

	List<ProductOrderReservation> findAllByOrderId(UUID orderId);
}
