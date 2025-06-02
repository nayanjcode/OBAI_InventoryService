package com.nayan.obai.inventory.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
public class RedissonConfig
{
	final Logger logger = LogManager.getLogger("RedissonConfig.java");
	@Bean(destroyMethod = "shutdown")
	public RedissonClient redissonClient() throws IOException
	{
		logger.info("loading redisson config");
		Config config = Config.fromYAML(
				new ClassPathResource("redisson-config.yml").getInputStream()
		);
		return Redisson.create(config);
	}
}
