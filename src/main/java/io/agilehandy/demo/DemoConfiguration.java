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
import com.mongodb.reactivestreams.client.MongoClient;
import io.agilehandy.demo.domain.Serialization;
import org.occurrent.eventstore.mongodb.spring.reactor.EventStoreConfig;
import org.occurrent.eventstore.mongodb.spring.reactor.SpringReactorMongoEventStore;
import org.occurrent.mongodb.timerepresentation.TimeRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory;

import java.net.URI;

/**
 * @author Haytham Mohamed
 **/

// run a local mongodb replicaset, from ./replica folder run $docker-compose up -d

@Configuration
public class DemoConfiguration {

	@Bean
	public Serialization serialization(ObjectMapper objectMapper) {
		return new Serialization(objectMapper, URI.create("urn:agilehandy:domain:account"));
	}


	@Configuration
	static class MongodbConfiguration {
		@Value("${spring.data.mongodb.database:database}")
		private String database;

		private String COLLECTION_NAME = "accounts";

		@Bean
		public ReactiveMongoTransactionManager mongoTransactionManager(MongoClient mongoClient) {
			return new ReactiveMongoTransactionManager(new SimpleReactiveMongoDatabaseFactory(mongoClient, database));
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



}
