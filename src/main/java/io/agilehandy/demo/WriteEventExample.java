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

import io.agilehandy.demo.events.AccountDeposited;
import io.agilehandy.demo.events.AccountEvent;
import io.agilehandy.demo.events.AccountOpened;
import io.agilehandy.demo.events.AccountWithdrew;
import io.agilehandy.demo.events.Serialization;
import io.cloudevents.CloudEvent;
import org.occurrent.eventstore.api.reactor.EventStore;
import org.occurrent.eventstore.api.reactor.EventStream;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Random;
import java.util.UUID;

/**
 * @author Haytham Mohamed
 **/

//@Component
public class WriteEventExample implements ApplicationRunner {

	private final EventStore eventStore;
	private final Serialization serialization;

	public WriteEventExample(EventStore eventStore, Serialization serialization) {
		this.eventStore = eventStore;
		this.serialization = serialization;
	}

	@Override
	public void run(ApplicationArguments args) {
		// create a new account event
		UUID accountId = UUID.randomUUID();
		Long customerId = new Random().nextLong();
		AccountOpened open = new AccountOpened();
		open.setCustomerId(customerId);
		open.setAccountId(accountId);
		open.setAmount(new Double(100));

		// withdraw some money
		AccountWithdrew withdrew = new AccountWithdrew();
		withdrew.setAccountId(accountId);
		withdrew.setAmount(new Double(10));

		// deposit some money
		AccountDeposited deposited = new AccountDeposited();
		deposited.setAccountId(accountId);
		deposited.setAmount(new Double(30));

		// Write
		Mono<Void> openMono = eventStore.write(customerId.toString(),
				Flux.just(serialization.serialize(open)));

		Mono<Void> withdrawMono = eventStore.write(customerId.toString(),
				Flux.just(serialization.serialize(withdrew)));

		Mono<Void> depositMono = eventStore.write(customerId.toString(),
				Flux.just(serialization.serialize(deposited)));

		// Read
		Mono<EventStream<CloudEvent>> eventStream = eventStore.read(customerId.toString());

		openMono
				.then(withdrawMono)
				.then(depositMono)
				.then(eventStream)
				.flatMapMany(es -> es.events())
				.map(serialization::deserialize)
				.map(this::toString)
				.doOnNext(System.out::println)
				.subscribe();
	}

	public String toString(AccountEvent e) {
		return "EventId: " + e.getEventId()
				+", AccountId: " + e.getAccountId()
				+", Activity: " + e.getActivity()
				+", Amount: " + e.getAmount()
				+", Time: " + e.getTime();
	}

}
