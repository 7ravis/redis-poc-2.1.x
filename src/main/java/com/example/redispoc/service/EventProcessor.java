package com.example.redispoc.service;

import com.example.redispoc.dto.EventDto;

public interface EventProcessor {

	/**
	 * @param event the event to be processed
	 * @throws Exception if processing cannot be completed successfully
	 */
	void processEvent(EventDto event) throws Exception;

}
