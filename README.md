# redis-poc-2.1.x

## Overview
This project contains working Spring Boot prototypes for two reliable event-processing patterns: [circular list](https://redis.io/commands/rpoplpush#pattern-circular-list) and [reliable queue](https://redis.io/commands/rpoplpush#pattern-reliable-queue).

## Pre-requisites
* [install and run Redis](https://redis.io/download)
* knowledge of Java and Spring Boot

## Seeing the circular list in action
1. [start redis](https://redis.io/topics/quickstart#starting-redis), connect to the [redis-cli](https://redis.io/topics/rediscli), and run the [monitor](https://redis.io/commands/monitor) command
1. [run this project](https://docs.spring.io/spring-boot/docs/current/reference/html/using-spring-boot.html#using-boot-running-your-application)
1. using [Postman](https://www.postman.com/), [curl](https://curl.haxx.se/), or your preferred API, POST events to http://localhost:8080/event. The request body is JSON and simply has a "name" field, e.g.
```
{
    "name": "foo"
}
```

## Seeing the reliable queue in action
1. In the project, open the com.example.redispoc.config.AppConfig class, comment out the eventWorker that uses a CircularListEventWorker, and uncomment the eventWorker that uses the ReliableQueueEventWorker.
1. Use the same steps from the circular list above. For testing purposes, there is a second endpoint to simulate events that have failed processing: simply send the same POST request as above to http://localhost:8080/event/processing (NOTE: this endpoint is N/A for the circular list).

## Picking a design pattern
I would recommend starting with the circular list as MVP because it the design is simpler and, thus, easier to maintain and avoid bugs. However, this circular list implementation does not work with concurrent event processing tasks, so if it is known upfront that a single thread cannot process events fast enough, I would go with the reliable queue (NOTE: the task configuration would need to be modified to run order processing tasks concurrently). WARNING: only the reliable queue's event processing task (NOT the event re-submission task) can be concurrent.

## Other notes
I encourage you to view the commit history and browse the project files. They should help you understand how to implement the patterns in your own project, and I tried to keep them simple and human-readable.
