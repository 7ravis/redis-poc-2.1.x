package com.example.redispoc.service;

import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;

import com.example.redispoc.dto.EventDto;

@EnableAsync
public class ReliableQueueEventWorker implements EventWorker {

	private static final Logger log = LoggerFactory.getLogger(ReliableQueueEventWorker.class);

	private static String getKeyForProcessingLock(EventDto event) {
		return String.format("lock:%s:%s", EventDto.class.getName(), event.getId().toString());
	}

	private RedisTemplate<String, EventDto> redisTemplate;
	private String pendingEventsKey;
	private String processingEventsKey;
	private EventProcessor eventProcessor;

	private long processingTimeoutMillis;

	public ReliableQueueEventWorker(RedisTemplate<String, EventDto> redisTemplate, String pendingEventsKey,
			String processingEventsKey, EventProcessor eventProcessor, long processingTimeoutMillis) {
		this.redisTemplate = redisTemplate;
		this.pendingEventsKey = pendingEventsKey;
		this.processingEventsKey = processingEventsKey;
		this.eventProcessor = eventProcessor;
		this.processingTimeoutMillis = processingTimeoutMillis;
	}

	@Async
	@Scheduled(fixedDelay = 5000)
	public void processEvents() {
		try {
			// wait for event
			ListOperations<String, EventDto> listOps = redisTemplate.opsForList();
			EventDto event = listOps.rightPopAndLeftPush(pendingEventsKey, processingEventsKey);

			// return (and delay) if no orders available
			if (event == null)
				return;

			// save processing lock to prevent re-submission of event
			long minimumTimeLockedForProcessing = processingTimeoutMillis * 2;
			redisTemplate.opsForValue().set(getKeyForProcessingLock(event), event, minimumTimeLockedForProcessing);

			// process event
			ConditionFactory await = Awaitility.await().atMost(processingTimeoutMillis, TimeUnit.MILLISECONDS);
			await.until(() -> {
				eventProcessor.processEvent(event);
				return true;
			});
			log.info(String.format("EVENT PROCESSED: %s", event.toString()));

			// remove event from queue
			listOps.remove(processingEventsKey, -1, event);
			log.info(String.format("EVENT REMOVED FROM QUEUE: %s", event.toString()));

			// remove processing lock
			// NOTE: this is optional since the processing lock expires harmlessly
			redisTemplate.delete(getKeyForProcessingLock(event));

		} catch (Throwable ex) {
			log.error(String.format("An error occurred while processing events: type=%s message=%s",
					ex.getClass().toString(), ex.getMessage()));
		}
	}

	// WARNING: this task should never be run concurrently because moving the event
	// back to the pending list is accomplished with RPOPLPUSH, so it needs to be
	// reliable that the item evaluated remains at the end of the processing list.
	// One way to ensure non-concurrency is to use Spring's TaskScheduler with the
	// fixedDelay parameter (e.g. @Scheduled(fixedDelay = 100000)").
	@Async
	@Scheduled(fixedDelay = 10000)
	public void resubmitExpiredEvents() {
		// this loop runs until the oldest event still has a processing lock or the
		// processing list is empty
		while (true) {
			try {
				// get the oldest event from the processing list
				EventDto event = redisTemplate.opsForList().index(processingEventsKey, -1);

				// return (and delay) if no events in the processing list
				if (event == null)
					return;

				// check if the event still has a processing lock
				Boolean hasLock = redisTemplate.hasKey(getKeyForProcessingLock(event));

				// return (and delay) if the oldest event still has a processing lock
				if (hasLock)
					return;

				// resubmit the event for processing
				redisTemplate.opsForList().rightPopAndLeftPush(processingEventsKey, pendingEventsKey);
				log.info(String.format("EVENT RESUBMITTED FOR PROCESSING: %s", event.toString()));
			} catch (Throwable ex) {
				log.error("An error occurred while resubmitting expired events.", ex);
			}
		}
	}

}
