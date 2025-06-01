package com.nayan.obai.inventory.service.impl;

import com.nayan.obai.inventory.entity.Product;
import com.nayan.obai.inventory.entity.ProductOrderReservation;
import com.nayan.obai.inventory.exception.InventoryServiceException;
import com.nayan.obai.inventory.repository.ProductRepository;
import com.nayan.obai.inventory.repository.ReservationRepository;
import com.nayan.obai.inventory.rest.OrderProduct;
import com.nayan.obai.inventory.service.ProductService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ProductServiceImpl implements ProductService
{
	private RedissonClient redissonClient;

	private RLock rLock;

	@Autowired
	ProductRepository productRepository;

	@Autowired
	private ReservationRepository reservationRepository;

	public ProductServiceImpl(){}

	public ProductServiceImpl(RedissonClient redissonClient)
	{
		this.redissonClient = redissonClient;
	}

	public ProductServiceImpl(final ProductRepository productRepository, final ReservationRepository reservationRepository, final RedissonClient redissonClient, final RLock rLock)
	{
		this.productRepository = productRepository;
		this.reservationRepository = reservationRepository;
		this.redissonClient = redissonClient;
		this.rLock = rLock;
	}

	@Override
	public Product getProduct(final UUID productId)
	{
		final Product product = productRepository.findById(productId).orElseThrow(() -> new InventoryServiceException("The product you are trying to search does not exist in the inventory."));
		int reservedStocks = reservationRepository.getTotalReservedQuantityByProductId(productId);
		// setting available stocks
		product.setQuantity(product.getQuantity() - reservedStocks);
		return product;
	}

	@Override
	public Product saveProduct(final Product product)
	{
		return productRepository.save(product);
	}

	@Override
	public List<Product> getAllProducts()
	{
		return productRepository.findAll();
	}

	@Override
	public boolean validateAndReserveProduct(final OrderProduct orderProduct)
	{
		final Map<String, RLock> acquiredLocks = new HashMap<>();

		try
		{
			final List<Product> items = orderProduct.getProducts();
			// Acquire locks
			for (Product item : items)
			{
				final String lockKey = "lock:stock:" + item.getProductId().toString();
				final RLock lock = redissonClient.getLock(lockKey);
				final boolean success = lock.tryLock(3, 10, TimeUnit.SECONDS);
				if (!success)
				{
					throw new IllegalStateException("Could not acquire lock for product " + item.getProductId());
				}
				acquiredLocks.put(item.getProductId().toString(), lock);
			}

			// Validate and reserve
			for (Product item : items)
			{
				final Product stock = getProduct(item.getProductId());
				// get the product reserved quantity and minus it from item.quantity to get the available quantity
				if (stock.getQuantity() < item.getQuantity())
				{
					throw new IllegalArgumentException("Insufficient stock for " + item.getProductId());
				}
			}

			for (Product item : items)
			{
				reservationRepository.save(ProductOrderReservation.builder()
						.productId(item.getProductId())
						.orderId(orderProduct.getOrderId())
						.reservedQuantity(item.getQuantity())
						.timestamp(Instant.now().getEpochSecond())
						.build());
			}

			return true;

		} catch (Exception e)
		{
			// Rollback any partial reservations
			reservationRepository.deleteByOrderId(orderProduct.getOrderId());
			return false;

		} finally
		{
			// Always release locks
			for (RLock lock : acquiredLocks.values())
			{
				if (lock.isHeldByCurrentThread())
				{
					lock.unlock();
				}
			}
		}
	}

	/*
	 * on payment success
	 * Deduct the stock from Product.quantity with ReserveProductOrder.reserveQuantity
	 * Remove entry from ReserveProductOrder for orderId
	 *
	 * on payment fail
	 * remove entry from reserve product order for orderId
	 * */

	@Transactional
	@Override
	public void updateProductStockForSuccessfulOrder(final UUID orderId)
	{
		// get all the products of the reserved order
		final List<ProductOrderReservation> productOrderReservations = reservationRepository.findAllByOrderId(orderId);

		// update product stock
		productOrderReservations.forEach((productOrderReservation -> {
			productRepository.deductQuantityByProductId(productOrderReservation.getProductId(), productOrderReservation.getReservedQuantity());
		}));

		// remove reserved stock
		reservationRepository.deleteByOrderId(orderId);
	}

	@Override
	public void rollbackProductStockForUnsuccessfulOrder(final UUID orderId)
	{
		// remove reserved stock
		reservationRepository.deleteByOrderId(orderId);
	}

}
