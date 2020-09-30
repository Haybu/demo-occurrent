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
import lombok.Data;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * @author Haytham Mohamed
 **/

@Data
public class Account {

	public static Flux<AccountEvent> openAccount(Long customerId, UUID accountId, Double amount) {
		AccountOpened account = new AccountOpened();
		account.setCustomerId(customerId);
		account.setAccountId(accountId);
		account.setAmount(amount);
		return Flux.just(account);
	}

	public static Flux<AccountEvent> withdraw(Flux<AccountEvent> eventStream, Double amount) {
		return eventStream.last()
				.filter(e -> e.getAmount().doubleValue() > amount.doubleValue())
				.map(e -> {
					AccountEvent withdrew = new AccountWithdrew();
					withdrew.setAccountId(e.getAccountId());
					withdrew.setAmount(amount);
					withdrew.setCustomerId(e.getCustomerId());
					return withdrew;
				})
				.flux();
	}

	public static Flux<AccountEvent> deposit(Flux<AccountEvent> eventStream, Double amount) {
		return eventStream
				.elementAt(0)
				.map(e -> {
					AccountEvent deposit = new AccountDeposited();
					deposit.setAccountId(e.getAccountId());
					deposit.setAmount(amount);
					deposit.setCustomerId(e.getCustomerId());
					return deposit;
				})
				.flux();
	}

}
