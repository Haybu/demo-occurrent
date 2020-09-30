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
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import reactor.core.publisher.Mono;

/**
 * @author Haytham Mohamed
 **/

@Document
@Data
@ToString
@NoArgsConstructor
public class Snapshot {

	@Id
	private String accountId;

	private Long customerId;

	private Double balance;

	private Long version;

	public Mono<Snapshot> updateFrom(AccountEvent event, Long streamVersion) {
		this.accountId = event.getAccountId().toString();
		this.customerId = event.getCustomerId();
		this.version = streamVersion;
		switch (event.getActivity()) {
			case OPEN: balance = event.getAmount(); break;
			case WITHDRAW: balance =- event.getAmount(); break;
			case DEPOSIT: balance =+ event.getAmount(); break;
			case CLOSE: balance = new Double(0); break;
			default: throw new RuntimeException("Unsupported event type when refreshing a snapshot");
		};
		return Mono.just(this);
	}

}
