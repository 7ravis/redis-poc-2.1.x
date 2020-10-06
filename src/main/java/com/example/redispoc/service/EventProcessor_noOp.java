package com.example.redispoc.service;

import com.example.redispoc.dto.EventDto;

public class EventProcessor_noOp implements EventProcessor {

	@Override
	public void processEvent(EventDto event) throws Exception {
		// do nothing
	}

}
