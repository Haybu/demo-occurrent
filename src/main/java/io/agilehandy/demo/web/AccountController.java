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

import io.agilehandy.demo.domain.Account;
import io.agilehandy.demo.domain.AccountService;
import io.agilehandy.demo.events.AccountEvent;
import io.agilehandy.demo.events.Serialization;
import lombok.extern.slf4j.Slf4j;
import org.occurrent.eventstore.api.reactor.EventStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
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

	@PostMapping("/accounts")
	public Mono<ResponseEntity<?>> createAccount(@RequestBody AccountRequest request) {
		UUID accountId = UUID.randomUUID();
		return service
				.execute(request.getCustomerId().toString(), events -> Account.openAccount(request.getCustomerId(), accountId, request.getAmount()))
				.then(Mono.just(ResponseEntity.ok(accountId.toString())))
				;
	}

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

	@PutMapping("/accounts/withdraw/{accountId}")
	public Mono<ResponseEntity<?>> withdrawAccount(@RequestBody AccountRequest request) {
		UUID accountId = UUID.fromString(request.getAccountId());
		return service
				.execute(request.getCustomerId().toString(), events -> Account.withdraw(events, request.getAmount()))
		        .then(Mono.just(ResponseEntity.ok(request.getAmount() + " is withdrawn from account " + accountId.toString())));
	}

	@PutMapping("/accounts/deposit/{accountId}")
	public Mono<ResponseEntity<?>> depositAccount(@RequestBody AccountRequest request) {
		UUID accountId = UUID.fromString(request.getAccountId());
		return service
				.execute(request.getCustomerId().toString(), events -> Account.deposit(events, request.getAmount()))
				.then(Mono.just(ResponseEntity.ok(request.getAmount() + " is deposited from account " + accountId.toString())));
	}

}
