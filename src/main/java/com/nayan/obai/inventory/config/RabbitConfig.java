package com.nayan.obai.inventory.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig
{
	final Logger logger = LogManager.getLogger("RabbitConfig");
	public static final String PAYMENT_EXCHANGE = "payment.exchange";
	public static final String PAYMENT_RESULT_QUEUE = "payment.result.queue";
	public static final String PAYMENT_RESULT_ROUTING_KEY = "payment.result";

	@Bean
	public DirectExchange paymentExchange()
	{
		return new DirectExchange(PAYMENT_EXCHANGE);
	}

	@Bean
	public Queue paymentResultQueue()
	{
		logger.info("setting payment result queue");
		return new Queue(PAYMENT_RESULT_QUEUE, true); // durable
	}

	@Bean
	public Binding paymentResultBinding()
	{
		logger.info("binding payment result");
		return BindingBuilder
				.bind(paymentResultQueue())
				.to(paymentExchange())
				.with(PAYMENT_RESULT_ROUTING_KEY);
	}

	// for message converters
	@Bean
	public MessageConverter jsonMessageConverter()
	{
		return new Jackson2JsonMessageConverter();
	}

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory)
	{
		logger.info("Setting RabbitMQ template");
		final RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.setMessageConverter(jsonMessageConverter());
		return template;
	}

	@Bean
	public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
			ConnectionFactory connectionFactory,
			MessageConverter messageConverter
	)
	{
		logger.info("Setting RabbitMQ connection factory and message converter");
		final SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		factory.setConnectionFactory(connectionFactory);
		factory.setMessageConverter(messageConverter);
		return factory;
	}
}
