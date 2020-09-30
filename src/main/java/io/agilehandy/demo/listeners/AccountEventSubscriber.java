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
package io.agilehandy.demo.listeners;

import io.agilehandy.demo.events.Serialization;
import lombok.extern.slf4j.Slf4j;
import org.occurrent.subscription.util.reactor.ReactorSubscriptionWithAutomaticPositionPersistence;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationEventPublisher;
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
public class AccountEventSubscriber implements DisposableBean {

	private static final String SUBSCRIBER_ID = "test-app";
	private final ReactorSubscriptionWithAutomaticPositionPersistence subscriptionForMongoDB;
	private final Serialization serialization;
	private final ApplicationEventPublisher eventPublisher;
	private final AtomicReference<Disposable> subscription;

	public AccountEventSubscriber(ReactorSubscriptionWithAutomaticPositionPersistence subscriptionForMongoDB,
	                              Serialization serialization, ApplicationEventPublisher eventPublisher) {
		this.subscriptionForMongoDB = subscriptionForMongoDB;
		this.serialization = serialization;
		this.eventPublisher = eventPublisher;
		this.subscription = new AtomicReference<>();
	}

	@PostConstruct
	void startEventStreaming() {
		log.info("Subscribing with id {}", SUBSCRIBER_ID);
		Disposable disposable = subscriptionForMongoDB.subscribe(SUBSCRIBER_ID,
				event -> Mono.just(event)
						.map(serialization::deserialize)
						.doOnNext(eventPublisher::publishEvent)
						.then())
				.subscribe();
		subscription.set(disposable);
	}

	@Override
	public void destroy() throws Exception {
		log.info("Unsubscribing");
		subscription.get().dispose();
	}
}
