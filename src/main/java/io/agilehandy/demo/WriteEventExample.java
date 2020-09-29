/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agilehandy.demo;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import org.occurrent.eventstore.api.reactor.EventStore;
import org.occurrent.eventstore.api.reactor.EventStream;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * @author Haytham Mohamed
 **/

@Component
public class WriteEventExample implements ApplicationRunner {

	private final EventStore eventStore;
	//private final Converter converter;

	public WriteEventExample(EventStore eventStore) {
		this.eventStore = eventStore;
	}


	@Override
	public void run(ApplicationArguments args) throws Exception {

		CloudEvent event = CloudEventBuilder.v1()
				.withId(UUID.randomUUID().toString())
				.withSource(URI.create("io.agilehandy.demo"))
				.withType("HelloWorld")
				.withTime(LocalDateTime.now().atOffset(ZoneOffset.UTC))
				.withSubject("demo")
				.withDataContentType("application/json")
				.withData("{ \"message\" : \"hello\" }".getBytes())
				.build();

		// Write
		Mono<Void> mono = eventStore.write("stream", Flux.just(event));

		// Read
		Mono<EventStream<CloudEvent>> eventStream = eventStore.read("stream");

		mono.then(eventStream)
				.flatMapMany(es -> es.events())
				.map(e -> new String(e.getData(), StandardCharsets.UTF_8))
				.doOnNext(System.out::println)
				.subscribe();
	}

}
