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

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.occurrent.eventstore.mongodb.spring.reactor.EventStoreConfig;
import org.occurrent.eventstore.mongodb.spring.reactor.SpringReactorMongoEventStore;
import org.occurrent.mongodb.timerepresentation.TimeRepresentation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory;

/**
 * @author Haytham Mohamed
 **/

@Configuration
public class MongodbConfiguration {

	// from ./replica folder run $docker-compose up -d
	private final String URL = "mongodb://localhost:27017/database?replicaSet=rs0";
	private final String DATABASE_NAME = "database";
	private final String COLLECTION_NAME = "events";

	@Bean
	public MongoClient mongoClient() {
		return MongoClients.create(URL);
	}

	@Bean
	public ReactiveMongoTransactionManager mongoTransactionManager(MongoClient mongoClient) {
		return new ReactiveMongoTransactionManager(new SimpleReactiveMongoDatabaseFactory(mongoClient, DATABASE_NAME));
	}

	@Bean
	public ReactiveMongoTemplate mongoTemplate(MongoClient mongoClient) {
		return new ReactiveMongoTemplate(mongoClient, DATABASE_NAME);
	}

	@Bean
	public EventStoreConfig eventStoreConfig(ReactiveMongoTransactionManager reactiveMongoTransactionManager) {
		return new EventStoreConfig.Builder()
				// The collection where all events will be stored
				.eventStoreCollectionName(COLLECTION_NAME)
				.transactionConfig(reactiveMongoTransactionManager)
				// How the CloudEvent "time" property will be serialized in MongoDB! !!Important!!
				.timeRepresentation(TimeRepresentation.RFC_3339_STRING)
				.build();
	}

	@Bean
	public SpringReactorMongoEventStore springReactorMongoEventStore(ReactiveMongoTemplate mongoTemplate, EventStoreConfig eventStoreConfig) {
		return new SpringReactorMongoEventStore(mongoTemplate, eventStoreConfig);
	}

}
