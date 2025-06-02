package com.nayan.obai.inventory.listener;

import com.nayan.obai.inventory.event.PaymentResultEvent;
import com.nayan.obai.inventory.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class PaymentResultListenerTest
{
	@Mock
	private ProductService productService;

	@InjectMocks
	private PaymentResultListener listener;

//	@BeforeEach
//	void setUp() {
//		listener = new PaymentResultListener(productService);
//	}

	@Test
	void testHandlePaymentSuccessWhenSuccessful() {
		// Arrange
		final UUID orderId = UUID.randomUUID();
		final PaymentResultEvent event = PaymentResultEvent.builder().orderId(orderId).isSuccessful(true).build();

		// Act
		listener.handlePaymentSuccess(event);

		// Assert
		Mockito.verify(productService, Mockito.times(1)).updateProductStockForSuccessfulOrder(orderId);
		Mockito.verify(productService, Mockito.never()).removeReservedProductStock(orderId);
	}

	@Test
	void testHandlePaymentSuccessWhenUnsuccessful() {
		// Arrange
		final UUID orderId = UUID.randomUUID();
		final PaymentResultEvent event = PaymentResultEvent.builder().orderId(orderId).isSuccessful(false).build();

		// Act
		listener.handlePaymentSuccess(event);

		// Assert
		Mockito.verify(productService, Mockito.times(1)).removeReservedProductStock(orderId);
		Mockito.verify(productService, Mockito.never()).updateProductStockForSuccessfulOrder(orderId);
	}
}