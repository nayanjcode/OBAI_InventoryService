package com.nayan.obai.inventory.controller;

import com.nayan.obai.inventory.entity.Product;
import com.nayan.obai.inventory.rest.OrderProduct;
import com.nayan.obai.inventory.service.ProductService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/inventory")
public class InventoryController
{
	final Logger logger = LogManager.getLogger("InventoryController");

	@Autowired
	private ProductService productService;

	@PreAuthorize("hasAuthority('SCOPE_internal') || hasRole('REGULAR_USERS')")
	@GetMapping("/{productId}")
	public ResponseEntity<Product> getProduct(@PathVariable UUID productId)
	{
		logger.debug("request for getProduct");
		final Product product = productService.getProduct(productId);
		return ResponseEntity.ok(product);
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/")
	public ResponseEntity<Product> saveProduct(@RequestBody Product product)
	{
		logger.debug("request for saveProduct");
//		this id handling is not required as Id itself does not seem to be required
//		try
//		{
//			final Product existingProduct = getProduct(product.getProductId()).getBody();
//			product.setId(existingProduct.getId());
//		} catch (Exception e)
//		{
//		}
		final Product updatedProduct = productService.saveProduct(product);
		return ResponseEntity.status(HttpStatus.CREATED).body(updatedProduct);
	}

	@PreAuthorize("hasRole('REGULAR_USERS')")
	@GetMapping("/")
	public ResponseEntity<List<Product>> getProducts()
	{
		logger.debug("request for get all products");
		final List<Product> products = productService.getAllProducts();
		return ResponseEntity.ok(products);
	}

	@PreAuthorize("hasAuthority('SCOPE_internal') || hasRole('REGULAR_USER')")
	@PostMapping("/validate")
	public ResponseEntity<Boolean> validateAndReserveProductStock(@RequestBody OrderProduct orderProduct)
	{
		logger.debug("request for validating product and reserving stock by locking");
		boolean isValid = productService.validateAndReserveProduct(orderProduct);
		return ResponseEntity.status(HttpStatus.OK).body(isValid);
	}
}
