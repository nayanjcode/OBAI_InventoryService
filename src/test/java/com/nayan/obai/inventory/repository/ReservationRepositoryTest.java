package com.nayan.obai.inventory.repository;

import com.nayan.obai.inventory.entity.ProductOrderReservation;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReservationRepositoryTest
{

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private ReservationRepository reservationRepository;

	@Test
	void testTotalReservedQuantityIfNotReserved(){
		assertTrue(reservationRepository.getTotalReservedQuantityByProductId(UUID.randomUUID()) == 0);
	}

	@Test
	void testTotalReservedQuantityIfReservedWithOneOrder(){
		final UUID productId = UUID.randomUUID();
		int reservedQuantityForOrder = 10;
		final ProductOrderReservation productReservationForOrder = ProductOrderReservation.builder()
				.orderId(UUID.randomUUID())
				.productId(productId)
				.reservedQuantity(reservedQuantityForOrder)
				.build();
		reservationRepository.save(productReservationForOrder);

		flushAndClear();

		int expectedReservedQuantity = reservedQuantityForOrder;
		int actualReservedQuantity = reservationRepository.getTotalReservedQuantityByProductId(productId);
		assertEquals(expectedReservedQuantity, actualReservedQuantity);
	}

	@Test
	void testTotalReservedQuantityIfReservedWithMultipleOrders(){
		final UUID productId = UUID.randomUUID();
		int reservedQuantityForOrder1 = 10;
		final ProductOrderReservation productReservationForOrder1 = ProductOrderReservation.builder()
				.orderId(UUID.randomUUID())
				.productId(productId)
				.reservedQuantity(reservedQuantityForOrder1)
				.build();
		reservationRepository.save(productReservationForOrder1);

		int reservedQuantityForOrder2 = 25;
		final ProductOrderReservation productReservationForOrder2 = ProductOrderReservation.builder()
				.orderId(UUID.randomUUID())
				.productId(productId)
				.reservedQuantity(reservedQuantityForOrder2)
				.build();
		reservationRepository.save(productReservationForOrder2);

		flushAndClear();

		int expectedReservedQuantity = reservedQuantityForOrder1 + reservedQuantityForOrder2;
		int actualReservedQuantity = reservationRepository.getTotalReservedQuantityByProductId(productId);
		assertEquals(expectedReservedQuantity, actualReservedQuantity);
	}

	@Test
	void testFindAllByOrderIdIfNoOrder() {
		assertTrue(reservationRepository.findAllByOrderId(UUID.randomUUID()).isEmpty());
	}

	@Test
	void testAllReservedOrderId() {
		final UUID orderId = UUID.randomUUID();
		int reservedQuantityForOrder1 = 10;
		final ProductOrderReservation productReservationForOrder1 = ProductOrderReservation.builder()
				.orderId(orderId)
				.productId(UUID.randomUUID())
				.reservedQuantity(reservedQuantityForOrder1)
				.build();
		reservationRepository.save(productReservationForOrder1);

		int reservedQuantityForOrder2 = 25;
		final ProductOrderReservation productReservationForOrder2 = ProductOrderReservation.builder()
				.orderId(orderId)
				.productId(UUID.randomUUID())
				.reservedQuantity(reservedQuantityForOrder2)
				.build();
		reservationRepository.save(productReservationForOrder2);

		flushAndClear();

		List<ProductOrderReservation> actualProductOrderReservationList = reservationRepository.findAllByOrderId(orderId);
		List<ProductOrderReservation> expectedReservations = List.of(productReservationForOrder1, productReservationForOrder2);
		assertEquals(expectedReservations, actualProductOrderReservationList);
	}


	@Test
	void deleteByOrderId(){
		final UUID orderId = UUID.randomUUID();
		int reservedQuantityForOrder1 = 10;
		final ProductOrderReservation productReservationForOrder1 = ProductOrderReservation.builder()
				.orderId(orderId)
				.productId(UUID.randomUUID())
				.reservedQuantity(reservedQuantityForOrder1)
				.build();
		reservationRepository.save(productReservationForOrder1);

		int reservedQuantityForOrder2 = 25;
		final ProductOrderReservation productReservationForOrder2 = ProductOrderReservation.builder()
				.orderId(orderId)
				.productId(UUID.randomUUID())
				.reservedQuantity(reservedQuantityForOrder2)
				.build();
		reservationRepository.save(productReservationForOrder2);

		flushAndClear();

		reservationRepository.deleteByOrderId(orderId);;

		flushAndClear();

		List<ProductOrderReservation> actualProductOrderReservationList = reservationRepository.findAllByOrderId(orderId);

		assertTrue(actualProductOrderReservationList.isEmpty());
	}

	private void flushAndClear() {
		// save values to DB. Currectly the values are not yet saved due to transaction.
		entityManager.flush();
		// remove JPA context so that we get data from DB and and not cache
		entityManager.clear();
	}
}