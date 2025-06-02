package com.nayan.obai.inventory.service.impl;

import com.nayan.obai.inventory.entity.Product;
import com.nayan.obai.inventory.entity.ProductOrderReservation;
import com.nayan.obai.inventory.exception.InventoryServiceException;
import com.nayan.obai.inventory.repository.ProductRepository;
import com.nayan.obai.inventory.repository.ReservationRepository;
import com.nayan.obai.inventory.rest.OrderProduct;
import com.nayan.obai.inventory.service.ProductService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ProductServiceImpl implements ProductService
{
	final Logger logger = LogManager.getLogger("ProductServiceImpl");
	private RedissonClient redissonClient;

	private RLock rLock;

	@Autowired
	ProductRepository productRepository;

	@Autowired
	private ReservationRepository reservationRepository;

	public ProductServiceImpl(){}

	@Autowired
	public ProductServiceImpl(final RedissonClient redissonClient)
	{
		logger.debug("injecting redisson client in Product Service");
		this.redissonClient = redissonClient;
	}

	public ProductServiceImpl(final ProductRepository productRepository, final ReservationRepository reservationRepository, final RedissonClient redissonClient, final RLock rLock)
	{
		logger.debug("injecting all dependencies(ProductRepository, ReservationRepository, RedissonClient, RLock) in Product Service");
		this.productRepository = productRepository;
		this.reservationRepository = reservationRepository;
		this.redissonClient = redissonClient;
		this.rLock = rLock;
	}

	@Override
	public Product getProduct(final UUID productId)
	{
		logger.info("fetching product for productId=" + productId);
		final Product product = productRepository.findById(productId).orElseThrow(() -> new InventoryServiceException("The product you are trying to search does not exist in the inventory."));
		int reservedStocks = reservationRepository.getTotalReservedQuantityByProductId(productId);
		// setting available stocks
		product.setQuantity(product.getQuantity() - reservedStocks);
		logger.info("Product:" + product);
		return product;
	}

	@Override
	public Product saveProduct(final Product product)
	{
		logger.info("saving product=" + product);
		final Product savedProduct = productRepository.save(product);
		logger.debug("saved product");
		return product;
	}

	@Override
	public List<Product> getAllProducts()
	{
		logger.info("fetching all the products");
		return productRepository.findAll();
	}

	@Override
	public boolean validateAndReserveProduct(final OrderProduct orderProduct)
	{
		logger.debug("validating and locking stocks");
		final Map<String, RLock> acquiredLocks = new HashMap<>();
		try
		{
			final List<Product> items = orderProduct.getProducts();
			// Acquire locks
			logger.debug("trying to acquire locks for order " + orderProduct.getOrderId());
			for (Product item : items)
			{
				final String lockKey = "lock:stock:" + item.getProductId().toString();
				logger.info("trying to get lock instance for " + lockKey);
				final RLock lock = redissonClient.getLock(lockKey);
				logger.info("trying to get lock for " + lockKey);
				final boolean success = lock.tryLock(3, 10, TimeUnit.SECONDS);
				if (!success)
				{
					throw new IllegalStateException("Could not acquire lock for product " + item.getProductId());
				}
				acquiredLocks.put(item.getProductId().toString(), lock);
				logger.info("acquired lock for " + lockKey);
			}
			logger.debug("acquired locks for prodcts of orderId=" + orderProduct.getOrderId());

			logger.debug("Validating stock for products of orderId=" + orderProduct.getOrderId());
			// Validate and reserve
			for (Product item : items)
			{
				final Product stock = getProduct(item.getProductId());
				// get the product reserved quantity and minus it from item.quantity to get the available quantity
				if (stock.getQuantity() < item.getQuantity())
				{
					final String errorMsg = MessageFormat.format("Insufficient stock for productId={0}. Requested {1} stock but has {2}", item.getProductId(), item.getQuantity(), stock.getQuantity());
					throw new IllegalArgumentException(errorMsg);
				}
			}
			logger.debug("Stock validated. We have sufficient stock for orderId=" + orderProduct.getOrderId());

			logger.debug("Reserving Stock for orderId=" + orderProduct.getOrderId());
			for (Product item : items)
			{
				reservationRepository.save(ProductOrderReservation.builder()
						.productId(item.getProductId())
						.orderId(orderProduct.getOrderId())
						.reservedQuantity(item.getQuantity())
						.timestamp(Instant.now().getEpochSecond())
						.build());
			}
			logger.debug("Reserved Stock for orderId=" + orderProduct.getOrderId());
			return true;

		} catch (Exception e)
		{
			logger.error(e.getMessage());
			// Rollback any partial reservations
			logger.info("Rollback reservations");
			removeReservedProductStock(orderProduct.getOrderId());
			logger.info("Rollback reservation complete");
			return false;

		} finally
		{
			// Always release locks
			logger.debug("Releasing locks");
			for (RLock lock : acquiredLocks.values())
			{
				if (lock.isHeldByCurrentThread())
				{
					lock.unlock();
				}
			}
			logger.debug("locks released");
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
		logger.debug("Update stock for successful order");
		// get all the products of the reserved order
		final List<ProductOrderReservation> productOrderReservations = reservationRepository.findAllByOrderId(orderId);

		// update product stock
		logger.debug("Deduct the product quantity for successful operation");
		productOrderReservations.forEach((productOrderReservation -> {
			productRepository.deductQuantityByProductId(productOrderReservation.getProductId(), productOrderReservation.getReservedQuantity());
		}));

		// remove reserved stock
		logger.debug("Remove reserved stock");
		removeReservedProductStock(orderId);
	}

	@Override
	public void removeReservedProductStock(final UUID orderId)
	{
		logger.info("Remove reservations for orderId=" + orderId);
		// remove reserved stock
		reservationRepository.deleteByOrderId(orderId);
		logger.info("Rollback reservation complete for orderId=" + orderId);

	}

}
