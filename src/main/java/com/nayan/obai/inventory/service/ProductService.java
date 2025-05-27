package com.nayan.obai.inventory.service;

import com.nayan.obai.inventory.entity.Product;
import com.nayan.obai.inventory.rest.OrderProduct;

import java.util.List;
import java.util.UUID;

public interface ProductService
{
	Product getProduct(UUID productId);

	List<Product> getAllProducts();

	Product saveProduct(Product product);

	boolean validateAndReserveProduct(OrderProduct orderProduct);

	void updateProductStockForSuccessfulOrder(final UUID orderId);

	void rollbackProductStockForUnsuccessfulOrder(final UUID orderId);

}
