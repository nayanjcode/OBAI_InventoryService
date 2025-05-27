package com.nayan.obai.inventory.listener;

import com.nayan.obai.inventory.config.RabbitConfig;
import com.nayan.obai.inventory.event.PaymentResultEvent;
import com.nayan.obai.inventory.service.ProductService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentResultListener
{
	ProductService productService;

	public PaymentResultListener(ProductService productService) {
		this.productService = productService;
	}

	@RabbitListener(queues = RabbitConfig.PAYMENT_RESULT_QUEUE)
	public void handlePaymentSuccess(final PaymentResultEvent event) {
		if(event.isSuccessful()){
			productService.updateProductStockForSuccessfulOrder(event.getOrderId());
		} else {
			productService.rollbackProductStockForUnsuccessfulOrder(event.getOrderId());
		}
	}
}
