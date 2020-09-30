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

import io.agilehandy.demo.events.Serialization;
import lombok.extern.slf4j.Slf4j;
import org.occurrent.eventstore.api.reactor.EventStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * @author Haytham Mohamed
 **/

@RestController
@Slf4j
public class AccountController {

	private final EventStore eventStore;
	private final AccountService service;
	private final Serialization serialization;

	public AccountController(EventStore eventStore, AccountService service, Serialization serialization) {
		this.eventStore = eventStore;
		this.service = service;
		this.serialization = serialization;
	}

	@GetMapping("/ping")
	public Mono<String> ping() { return Mono.just("pong"); }

	@PostMapping("/accounts")
	public Mono<ResponseEntity<?>> createAccount(@RequestBody AccountRequest request) {
		log.info("creating a new account");
		UUID accountId = UUID.randomUUID();
		return service
				.execute(accountId.toString(), request.getCustomerId(), events -> Account.openAccount(accountId, request.getCustomerId(), request.getAmount()))
				.then(Mono.just(ResponseEntity.ok(accountId.toString())))
				;
	}

	/** in CQRS reading is a different route
	@GetMapping("/accounts/{customerId}")
	public Flux<AccountEvent> accounts(@PathVariable Long customerId) {
		log.info("reading accounts for customer id: " + customerId);
		return eventStore.read(customerId.toString())
				.flatMapMany(es -> es.events())
				.map(serialization::deserialize)
				;
	}

	@GetMapping("/accounts/{customerId}/{accountId}")
	public Flux<AccountEvent> accounts(@PathVariable Long customerId, @PathVariable String accountId) {
		log.info("reading account " + accountId + " for customer id: " + customerId);
		return this.accounts(customerId)
				.filter(e -> e.getAccountId().equals(UUID.fromString(accountId)))
				;
	}
	 **/

	@PutMapping("/accounts/withdraw/{accountId}")
	public Mono<ResponseEntity<?>> withdrawAccount(@RequestBody AccountRequest request) {
		return service
				.execute(request.getAccountId(), request.getCustomerId(), snapshot -> Account.withdraw(snapshot, request.getAmount()))
		        .then(Mono.just(ResponseEntity.ok(request.getAmount() + " is withdrawn from account " + request.getAccountId())));
	}

	@PutMapping("/accounts/deposit/{accountId}")
	public Mono<ResponseEntity<?>> depositAccount(@RequestBody AccountRequest request) {
		return service
				.execute(request.getAccountId(), request.getCustomerId(), snapshot -> Account.deposit(snapshot, request.getAmount()))
				.then(Mono.just(ResponseEntity.ok(request.getAmount() + " is deposited from account " + request.getAccountId())));
	}

}
