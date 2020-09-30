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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.mongodb.reactivestreams.client.MongoClient;
import io.agilehandy.demo.events.Serialization;
import org.occurrent.eventstore.mongodb.spring.reactor.EventStoreConfig;
import org.occurrent.eventstore.mongodb.spring.reactor.SpringReactorMongoEventStore;
import org.occurrent.mongodb.timerepresentation.TimeRepresentation;
import org.occurrent.subscription.api.reactor.PositionAwareReactorSubscription;
import org.occurrent.subscription.api.reactor.ReactorSubscriptionPositionStorage;
import org.occurrent.subscription.mongodb.spring.reactor.SpringReactorSubscriptionForMongoDB;
import org.occurrent.subscription.mongodb.spring.reactor.SpringReactorSubscriptionPositionStorageForMongoDB;
import org.occurrent.subscription.util.reactor.ReactorSubscriptionWithAutomaticPositionPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

import java.net.URI;

import static com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.*;

/**
 * @author Haytham Mohamed
 **/

// run a local mongodb replicaset, from ./replica folder run $docker-compose up -d

@Configuration
@EnableReactiveMongoRepositories(basePackages = "io.agilehandy.demo.snapshot")
public class DemoConfiguration {

	@Bean
	public Serialization serialization(ObjectMapper objectMapper) {
		return new Serialization(objectMapper, URI.create("urn:agilehandy:domain:account"));
	}

	//@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		// Configure jackson to add type information to each serialized object
		// Allows deserializing interfaces such as DomainEvent
		objectMapper.activateDefaultTyping(new LaissezFaireSubTypeValidator(), EVERYTHING);
		return objectMapper;
	}

	@Configuration
	static class MongodbConfiguration {
		@Value("${spring.data.mongodb.database:database}")
		private String database;

		@Value("${mongo.collections.events:accounts}")
		private String collection;

		@Value("${mongo.collections.positions:subscriptions}")
		private String positions;

		@Bean
		public ReactiveMongoTransactionManager mongoTransactionManager(MongoClient mongoClient) {
			return new ReactiveMongoTransactionManager(new SimpleReactiveMongoDatabaseFactory(mongoClient, database));
		}

		@Bean
		public EventStoreConfig eventStoreConfig(ReactiveMongoTransactionManager reactiveMongoTransactionManager) {
			return new EventStoreConfig.Builder()
					// The collection where all events will be stored
					.eventStoreCollectionName(collection)
					.transactionConfig(reactiveMongoTransactionManager)
					// How the CloudEvent "time" property will be serialized in MongoDB! !!Important!!
					.timeRepresentation(TimeRepresentation.RFC_3339_STRING)
					.build();
		}

		@Bean
		public SpringReactorMongoEventStore springReactorMongoEventStore(ReactiveMongoTemplate mongoTemplate, EventStoreConfig eventStoreConfig) {
			return new SpringReactorMongoEventStore(mongoTemplate, eventStoreConfig);
		}

		@Bean
		public ReactorSubscriptionPositionStorage reactorSubscriptionPositionStorage(ReactiveMongoOperations mongoOperations) {
			return new SpringReactorSubscriptionPositionStorageForMongoDB(mongoOperations, positions);
		}

		@Bean
		public PositionAwareReactorSubscription subscription(ReactiveMongoOperations mongoOperations) {
			return new SpringReactorSubscriptionForMongoDB(mongoOperations, collection, TimeRepresentation.RFC_3339_STRING);
		}

		@Bean
		public ReactorSubscriptionWithAutomaticPositionPersistence autoPersistingSubscription(PositionAwareReactorSubscription subscription, ReactorSubscriptionPositionStorage storage) {
			return new ReactorSubscriptionWithAutomaticPositionPersistence(subscription, storage);
		}
	}



}
