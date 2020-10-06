package com.example.redispoc.service;

import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import com.example.redispoc.dto.EventDto;

public class CircularListEventWorker implements EventWorker {

	private static final Logger log = LoggerFactory.getLogger(CircularListEventWorker.class);

	private ListOperations<String, EventDto> listOps;
	private String pendingEventsKey;
	private EventProcessor eventProcessor;
	private long processingTimeoutMillis;

	public CircularListEventWorker(RedisTemplate<String, EventDto> redisTemplate, String pendingEventsKey,
			EventProcessor eventProcessor, long processingTimeoutMillis) {
		this.listOps = redisTemplate.opsForList();
		this.pendingEventsKey = pendingEventsKey;
		this.eventProcessor = eventProcessor;
		this.processingTimeoutMillis = processingTimeoutMillis;
	}

	@Scheduled(fixedDelay = 1)
	public void processEvents() {
		try {
			// wait for event
			EventDto event = listOps.rightPopAndLeftPush(pendingEventsKey, pendingEventsKey, 0l, TimeUnit.SECONDS);

			// process event
			ConditionFactory await = Awaitility.await().atMost(processingTimeoutMillis, TimeUnit.MILLISECONDS);
			await.until(() -> {
				eventProcessor.processEvent(event);
				return true;
			});
			log.info(String.format("EVENT PROCESSED: %s", event.toString()));

			// remove event from queue
			listOps.remove(pendingEventsKey, -1, event);
			log.info(String.format("EVENT REMOVED FROM QUEUE: %s", event.toString()));

		} catch (Throwable ex) {
			log.error(String.format("An error occurred while processing events: type=%s message=%s",
					ex.getClass().toString(), ex.getMessage()));
		}
	}

}
