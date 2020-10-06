package com.example.redispoc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.example.redispoc.dto.EventDto;
import com.example.redispoc.service.CircularListEventWorker;
import com.example.redispoc.service.EventProcessor;
import com.example.redispoc.service.EventProcessor_noOp;
import com.example.redispoc.service.EventWorker;

@Configuration
@EnableScheduling
public class AppConfig {

	@Value("${redispoc.redis.keys.pendingevents}")
	private String pendingEventsKey;

	@Value("${redispoc.redis.keys.processingevents}")
	private String processingEventsKey;

	@Value("${redispoc.eventprocessing.timeoutMillis}")
	private long processingTimeoutMillis;

	@Bean
	public EventProcessor eventProcessor() {
		return new EventProcessor_noOp();
	}

	@Bean
	public EventWorker eventWorker() {
		return new CircularListEventWorker(redisTemplate(), pendingEventsKey, eventProcessor(),
				processingTimeoutMillis);
	}

//	@Bean
//	public EventWorker eventWorker() {
//		return new ReliableQueueEventWorker(redisTemplate(), pendingEventsKey, processingEventsKey, eventProcessor(),
//				processingTimeoutMillis);
//	}

	@Bean
	public LettuceConnectionFactory redisConnectionFactory() {
		return new LettuceConnectionFactory(new RedisStandaloneConfiguration());
	}

	@Bean
	public RedisTemplate<String, EventDto> redisTemplate() {
		RedisTemplate<String, EventDto> template = new RedisTemplate<>();
		template.setConnectionFactory(redisConnectionFactory());
		return template;
	}

}
