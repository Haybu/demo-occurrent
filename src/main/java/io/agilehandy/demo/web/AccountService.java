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
package io.agilehandy.demo.web;

import io.agilehandy.demo.events.AccountEvent;
import io.agilehandy.demo.events.Serialization;
import io.agilehandy.demo.snapshot.Snapshot;
import io.agilehandy.demo.snapshot.SnapshotRepository;
import io.cloudevents.CloudEvent;
import lombok.extern.slf4j.Slf4j;
import org.occurrent.eventstore.api.reactor.EventStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * @author Haytham Mohamed
 **/

@Service
@Slf4j
public class AccountService {

	private final SnapshotRepository snapshotRepository;
	private final EventStore eventStore;
	private final Serialization serialization;

	public AccountService(SnapshotRepository snapshotRepository, EventStore eventStore, Serialization serialization) {
		this.snapshotRepository = snapshotRepository;
		this.eventStore = eventStore;
		this.serialization = serialization;
	}

	// Here where events are written to streams.
	public Mono<Void> execute(String accountId, Long customerId, Function<Mono<Snapshot>, Mono<AccountEvent>> functionThatCallsDomainModel) {

		// Read all events from the event store for a particular stream
		Mono<Snapshot> snapshotMono = snapshotRepository.findById(accountId);

		// Call a pure function from the domain model which returns a Stream of domain events
		Mono<AccountEvent> newDomainEvent = functionThatCallsDomainModel.apply(snapshotMono);

		Mono<CloudEvent> cloudEventMono = newDomainEvent.map(serialization::serialize);

		String newStreamId = "stream" + customerId.toString();
		log.info("writing event to streamId: " + newStreamId);
		eventStore.write(newStreamId, cloudEventMono.flux()).subscribe();

		return Mono.empty();
	}
}
