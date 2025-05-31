package com.nayan.obai.inventory.service.impl;

import com.nayan.obai.inventory.entity.Product;
import com.nayan.obai.inventory.entity.ProductOrderReservation;
import com.nayan.obai.inventory.exception.InventoryServiceException;
import com.nayan.obai.inventory.repository.ProductRepository;
import com.nayan.obai.inventory.repository.ReservationRepository;
import com.nayan.obai.inventory.rest.OrderProduct;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest
{

	@Mock
	private ProductRepository productRepository;

	@Mock
	private ReservationRepository reservationRepository;

	@Mock
	private RedissonClient redissonClient;

	@Mock
	private RLock rLock;

	@InjectMocks
	private ProductServiceImpl productService;

//	@BeforeEach
//	void setup()
//	{
//		this.productService = new ProductServiceImpl(productRepository, reservationRepository, redissonClient, rLock);
//	}

	@Test
	void shouldReturnEmptyListWhenNoProductsExist() {
		Mockito.when(productRepository.findAll()).thenReturn(Collections.emptyList());

		final List<Product> productList = productService.getAllProducts();

		Assertions.assertTrue(productList.isEmpty());
		Mockito.verify(productRepository, Mockito.times(1)).findAll();
	}

	@Test
	void shouldReturnProductWhenItExists() {
		final UUID productId = UUID.randomUUID();
		final Product product = Product.builder().productId(productId).quantity(10).build();

		Mockito.when(productRepository.findById(productId)).thenReturn(Optional.of(product));

		final Product result = productService.getProduct(productId);

		Assertions.assertEquals(product, result);
		Mockito.verify(productRepository, Mockito.times(1)).findById(productId);
	}

	// Test for getProduct Throws Exception
	@Test
	void shouldThrowExceptionWhenProductNotFound() {
		final UUID productId = UUID.randomUUID();

		Mockito.when(productRepository.findById(productId)).thenReturn(Optional.empty());

		Assertions.assertThrows(InventoryServiceException.class, () -> {
			productService.getProduct(productId);
		});
	}


	// Test for getProduct()
	@Test
	void shouldReturnProductWithAvailableQuantity() {
		UUID productId = UUID.randomUUID();
		final int currentQuantity = 10;
		final Product product = Product.builder().productId(productId).quantity(currentQuantity).build();

		final int reservedQuantity = 3;
		Mockito.when(productRepository.findById(productId)).thenReturn(Optional.of(product));
		Mockito.when(reservationRepository.getTotalReservedQuantityByProductId(productId)).thenReturn(reservedQuantity);

		final Product result = productService.getProduct(productId);

		final int expectedQuantity = currentQuantity - reservedQuantity;
		Assertions.assertEquals(expectedQuantity, result.getQuantity());
	}

	// Test for saveProduct()
	@Test
	void shouldSaveAndReturnProduct() {
		final Product product = Product.builder().productId(UUID.randomUUID()).quantity(10).build();

		Mockito.when(productRepository.save(product)).thenReturn(product);

		final Product result = productService.saveProduct(product);

		Assertions.assertEquals(product, result);
	}

	// Test for getAllProducts()
	@Test
	void shouldReturnAllProducts() {
		final List<Product> products = List.of(
				Product.builder().productId(UUID.randomUUID()).quantity(10).build()
		);

		Mockito.when(productRepository.findAll()).thenReturn(products);

		final List<Product> result = productService.getAllProducts();

		Assertions.assertEquals(products, result);
	}

	// Test for validateAndReserveProduct() (Happy Path)
	@Test
	void shouldValidateAndReserveProductSuccessfully() throws Exception {
		final UUID productId = UUID.randomUUID();
		final UUID orderId = UUID.randomUUID();
		final Product requestProduct = Product.builder().productId(productId).quantity(5).build();
		final Product dbProduct = Product.builder().productId(productId).quantity(10).build();

		final OrderProduct orderProduct = OrderProduct.builder().orderId(orderId).products(List.of(requestProduct)).build();

		Mockito.when(redissonClient.getLock(ArgumentMatchers.anyString())).thenReturn(rLock);
		Mockito.when(rLock.tryLock(ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong(), ArgumentMatchers.any())).thenReturn(true);
		Mockito.when(rLock.isHeldByCurrentThread()).thenReturn(true);
		Mockito.when(productRepository.findById(productId)).thenReturn(Optional.of(dbProduct));
		Mockito.when(reservationRepository.getTotalReservedQuantityByProductId(productId)).thenReturn(2);

		final boolean result = productService.validateAndReserveProduct(orderProduct);

		Assertions.assertTrue(result);
		Mockito.verify(reservationRepository).save(ArgumentMatchers.any(ProductOrderReservation.class));
		Mockito.verify(rLock).unlock();
	}

	// Test validateAndReserveProduct() for Failure (Insufficient Stock)
	@Test
	void shouldReturnFalseWhenInsufficientStock() throws Exception {
		final UUID productId = UUID.randomUUID();
		final UUID orderId = UUID.randomUUID();
		final Product requestProduct = Product.builder().productId(productId).quantity(8).build();
		final Product dbProduct = Product.builder().productId(productId).quantity(10).build();

		final OrderProduct orderProduct = OrderProduct.builder().orderId(orderId).products(List.of(requestProduct)).build();

		Mockito.when(redissonClient.getLock(ArgumentMatchers.anyString())).thenReturn(rLock);
		Mockito.when(rLock.tryLock(ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong(), ArgumentMatchers.any())).thenReturn(true);
		Mockito.when(rLock.isHeldByCurrentThread()).thenReturn(true);
		Mockito.when(productRepository.findById(productId)).thenReturn(Optional.of(dbProduct));
		Mockito.when(reservationRepository.getTotalReservedQuantityByProductId(productId)).thenReturn(5);

		final boolean result = productService.validateAndReserveProduct(orderProduct);

		Assertions.assertFalse(result);
		Mockito.verify(reservationRepository).deleteByOrderId(orderId);
		Mockito.verify(rLock).unlock();
	}

	// Test validateAndReserveProduct for Failure (Lock Acquisition Fails)
	@Test
	void shouldReturnFalseWhenLockNotAcquired() throws Exception {
		final UUID productId = UUID.randomUUID();
		final UUID orderId = UUID.randomUUID();

		final Product requestProduct = Product.builder().productId(productId).quantity(5).build();
		final OrderProduct orderProduct = OrderProduct.builder().orderId(orderId).products(List.of(requestProduct)).build();

		Mockito.when(redissonClient.getLock(ArgumentMatchers.anyString())).thenReturn(rLock);
		Mockito.when(rLock.tryLock(ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong(), ArgumentMatchers.any())).thenReturn(false);

		final boolean result = productService.validateAndReserveProduct(orderProduct);

		Assertions.assertFalse(result);
		Mockito.verify(reservationRepository).deleteByOrderId(orderId);
	}

	// Test for updateProductStockForSuccessfulOrder()
	@Test
	void shouldUpdateStockAndDeleteReservationOnSuccess() {
		final UUID orderId = UUID.randomUUID();
		final UUID productId = UUID.randomUUID();

		final ProductOrderReservation reservation = ProductOrderReservation.builder()
				.orderId(orderId)
				.productId(productId)
				.reservedQuantity(4)
				.build();

		Mockito.when(reservationRepository.findAllByOrderId(orderId))
				.thenReturn(List.of(reservation));

		productService.updateProductStockForSuccessfulOrder(orderId);

		Mockito.verify(productRepository).deductQuantityByProductId(productId, 4);
		Mockito.verify(reservationRepository).deleteByOrderId(orderId);
	}

	// Test for rollbackProductStockForUnsuccessfulOrder()
	@Test
	void shouldDeleteReservationOnOrderFailure() {
		final UUID orderId = UUID.randomUUID();

		productService.rollbackProductStockForUnsuccessfulOrder(orderId);

		Mockito.verify(reservationRepository).deleteByOrderId(orderId);
	}

}