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
package io.agilehandy.demo.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static java.time.ZoneOffset.*;
import static java.util.Objects.*;

/**
 * @author Haytham Mohamed
 **/

public class Serialization {

	private final String AMOUNT_FIELD_NAME = "amount";
	private final String CUSTOMER_ID_FIELD_NAME = "customerId";

	private final ObjectMapper objectMapper;
	private final URI source;

	public Serialization(ObjectMapper objectMapper, URI source) {
		this.objectMapper = objectMapper;
		this.source = source;
	}

	public AccountEvent deserialize(CloudEvent c) {
		AccountEvent event;
		if (Objects.equals(c.getType(), AccountOpened.class.getSimpleName())) {
			event = new AccountOpened();
		} else if (Objects.equals(c.getType(), AccountClosed.class.getSimpleName())) {
			event = new AccountClosed();
		} else if (Objects.equals(c.getType(), AccountWithdrew.class.getSimpleName())) {
			event = new AccountWithdrew();
		} else if (Objects.equals(c.getType(), AccountDeposited.class.getSimpleName())) {
			event = new AccountDeposited();
		} else {
			throw new IllegalStateException("Unrecognized event: " + c.getType());
		}

		UUID eventId = UUID.fromString(c.getId());
		UUID accountId = UUID.fromString(requireNonNull(c.getSubject()));
		LocalDateTime time = requireNonNull(c.getTime()).toLocalDateTime();
		Map<String, Object> data = deserializeData(c);
		Double amount = (Double)data.get(AMOUNT_FIELD_NAME);
		Long customerId = Long.valueOf((Integer)data.get(CUSTOMER_ID_FIELD_NAME));

		event.setAccountId(accountId);
		event.setCustomerId(customerId);
		event.setEventId(eventId);
		event.setTime(time);
		event.setAmount(amount);
		return event;
	}

	public CloudEvent serialize(AccountEvent e) {
		return CloudEventBuilder.v1()
				.withId(e.getEventId().toString())
				.withSource(source)
				.withSubject(e.getAccountId().toString())
				.withType(e.getClass().getSimpleName())
				.withTime(e.getTime().atOffset(UTC).truncatedTo(ChronoUnit.MILLIS))
				.withDataContentType("application/json")
				.withData(toBytes(e))
				.build();
	}

	private Map<String, Object> deserializeData(CloudEvent c) {
		try {
			return objectMapper.readValue(c.getData(), new TypeReference<Map<String, Object>>() {
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private byte[] toBytes(AccountEvent event) {
		final Map<String, Object> eventAsMap = new HashMap<>();
		eventAsMap.put(AMOUNT_FIELD_NAME, event.getAmount());
		eventAsMap.put(CUSTOMER_ID_FIELD_NAME, event.getCustomerId());
		try {
			return objectMapper.writeValueAsBytes(eventAsMap);
		} catch (JsonProcessingException jsonProcessingException) {
			throw new RuntimeException(jsonProcessingException);
		}
	}

}
