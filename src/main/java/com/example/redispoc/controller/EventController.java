package com.example.redispoc.controller;

import java.util.UUID;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.redispoc.dto.CreateEventDto;
import com.example.redispoc.dto.EventDto;

@RestController
@RequestMapping("/event")
public class EventController {

	@Value("${redispoc.redis.keys.pendingevents}")
	private String pendingEventsKey;

	@Resource(name = "redisTemplate")
	private ListOperations<String, Object> listOps;

	@PostMapping
	@ResponseStatus(HttpStatus.ACCEPTED)
	EventDto lpush(@RequestBody CreateEventDto request) {
		UUID id = UUID.randomUUID();
		EventDto response = new EventDto();
		response.setName(request.getName());
		response.setId(id);
		listOps.leftPush(pendingEventsKey, response);
		return response;
	}

}
