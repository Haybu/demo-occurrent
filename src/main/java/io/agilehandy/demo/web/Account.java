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

import io.agilehandy.demo.events.AccountDeposited;
import io.agilehandy.demo.events.AccountEvent;
import io.agilehandy.demo.events.AccountOpened;
import io.agilehandy.demo.events.AccountWithdrew;
import io.agilehandy.demo.snapshot.Snapshot;
import lombok.Data;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * @author Haytham Mohamed
 *
 * Note: no state
 **/

@Data
public class Account {

	public static Mono<AccountEvent> openAccount(UUID accountId, Long customerId, Double amount) {
		AccountOpened account = new AccountOpened();
		account.setCustomerId(customerId);
		account.setAccountId(accountId);
		account.setAmount(amount);
		return Mono.just(account);
	}

	public static Mono<AccountEvent> withdraw(Mono<Snapshot> snapshot, Double amount) {
		return snapshot
				.filter(s -> s.getBalance().doubleValue() > amount.doubleValue())
				.map(s -> {
					AccountEvent withdrew = new AccountWithdrew();
					withdrew.setAccountId(UUID.fromString(s.getAccountId()));
					withdrew.setAmount(amount);
					withdrew.setCustomerId(s.getCustomerId());
					return withdrew;
				});
	}

	public static Mono<AccountEvent> deposit(Mono<Snapshot> snapshot, Double amount) {
		return snapshot
				.map(s -> {
					AccountEvent deposit = new AccountDeposited();
					deposit.setAccountId(UUID.fromString(s.getAccountId()));
					deposit.setAmount(amount);
					deposit.setCustomerId(s.getCustomerId());
					return deposit;
				});
	}

}
