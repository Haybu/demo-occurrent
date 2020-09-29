/*
 * Copyright 2013-2019 the original author or authors.
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
package io.agilehandy.demo.domain;

import io.agilehandy.demo.events.AccountEvent;
import io.agilehandy.demo.events.Serialization;
import io.cloudevents.CloudEvent;
import org.occurrent.eventstore.api.reactor.EventStore;
import org.occurrent.eventstore.api.reactor.EventStream;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * @author Haytham Mohamed
 **/

@Service
public class AccountService {

	private final EventStore eventStore;
	private final Serialization serialization;

	public AccountService(EventStore eventStore, Serialization serialization) {
		this.eventStore = eventStore;
		this.serialization = serialization;
	}

	public Mono<Void> execute(String streamId, Function<Flux<AccountEvent>, Flux<AccountEvent>> functionThatCallsDomainModel) {
		// Read all events from the event store for a particular stream
		Mono<EventStream<CloudEvent>> eventStream = eventStore.read(streamId);

		// Convert the cloud events into domain events
		Flux<AccountEvent> accountEventFlux = eventStream
				.flatMapMany(e -> e.events())
				.map(serialization::deserialize);

		// Call a pure function from the domain model which returns a Stream of domain events
		Flux<AccountEvent> newDomainEvents = functionThatCallsDomainModel.apply(accountEventFlux);

		Flux<CloudEvent> cloudEventFlux = newDomainEvents.map(serialization::serialize);

		eventStore.write(streamId, cloudEventFlux).subscribe();

		return Mono.empty();
	}
}
