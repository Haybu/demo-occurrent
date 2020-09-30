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
package io.agilehandy.demo.snapshot;

import io.agilehandy.demo.events.AccountEvent;
import io.agilehandy.demo.events.Serialization;
import lombok.extern.slf4j.Slf4j;
import org.occurrent.cloudevents.OccurrentExtensionGetter;
import org.occurrent.subscription.util.reactor.ReactorSubscriptionWithAutomaticPositionPersistence;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Haytham Mohamed
 **/

@Component
@Slf4j
public class SnapshotRefresh implements DisposableBean {

	private final SnapshotRepository snapshotRepository;
	private static final String SUBSCRIBER_ID = "updateSnapshots";
	private final ReactorSubscriptionWithAutomaticPositionPersistence subscriptionForMongoDB;
	private final Serialization serialization;
	private final AtomicReference<Disposable> subscription;

	public SnapshotRefresh(SnapshotRepository snapshotRepository, ReactorSubscriptionWithAutomaticPositionPersistence subscriptionForMongoDB, Serialization serialization) {
		this.snapshotRepository = snapshotRepository;
		this.subscriptionForMongoDB = subscriptionForMongoDB;
		this.serialization = serialization;
		this.subscription = new AtomicReference<>();
	}

	@PostConstruct
	public Mono<Void> updateSnapshotWhenNewEventsAreWrittenToEventStore() {
		Disposable disposable = subscriptionForMongoDB.subscribe(SUBSCRIBER_ID, cloudEvent -> {

			AccountEvent accountEvent = serialization.deserialize(cloudEvent);
			String streamId = OccurrentExtensionGetter.getStreamId(cloudEvent);
			long streamVersion = OccurrentExtensionGetter.getStreamVersion(cloudEvent);

			log.info("snapshot reading from streamId: " + streamId);
			log.info("update account with id: " + accountEvent.getAccountId());

			return snapshotRepository.findById(accountEvent.getAccountId().toString())
					.switchIfEmpty(Mono.just(new Snapshot()))
					.flatMap(s -> s.updateFrom(accountEvent, streamVersion))
					.flatMap(snapshotRepository::save)
					.doOnNext(s -> log.info("snapshot refreshed: " + s))
					//.then(snapshotRepository.findById(accountEvent.getAccountId().toString()))
					//.doOnNext(s -> log.info(s.toString()))
					.then();
		}).subscribe();

		subscription.set(disposable);
		return Mono.empty();
	}

	@Override
	public void destroy() throws Exception {
		log.info("Unsubscribing");
		subscription.get().dispose();
	}
}
