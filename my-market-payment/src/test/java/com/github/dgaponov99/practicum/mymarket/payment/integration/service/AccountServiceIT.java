package com.github.dgaponov99.practicum.mymarket.payment.integration.service;

import com.github.dgaponov99.practicum.mymarket.payment.exception.AccountAlreadyExistException;
import com.github.dgaponov99.practicum.mymarket.payment.exception.AccountNotFoundException;
import com.github.dgaponov99.practicum.mymarket.payment.exception.InsufficientBalanceException;
import com.github.dgaponov99.practicum.mymarket.payment.persistence.entity.Account;
import com.github.dgaponov99.practicum.mymarket.payment.persistence.repository.AccountRepository;
import com.github.dgaponov99.practicum.mymarket.payment.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

public class AccountServiceIT extends ServiceIT {

    @Autowired
    AccountService accountService;

    @Autowired
    AccountRepository accountRepository;

    @Test
    void create_success() {
        var account = accountService.create(1000000).block();

        accountRepository.findById(account.getId())
                .doOnNext(actualAccount -> {
                    assertThat(actualAccount)
                            .isNotNull()
                            .extracting(Account::getBalance)
                            .isEqualTo(1000000L);
                })
                .block();
    }

    @Test
    void create_alreadyExists() {
        accountRepository.save(new Account(null, 200000)).block();

        StepVerifier.create(accountService.create(1000000))
                .expectError(AccountAlreadyExistException.class)
                .verify();
    }

    @Test
    void account_success() {
        accountRepository.save(new Account(null, 200000)).block();

        accountService.account()
                .doOnNext(actualAccount -> {
                    assertThat(actualAccount)
                            .isNotNull()
                            .extracting(Account::getBalance)
                            .isEqualTo(200000L);
                })
                .block();
    }

    @Test
    void account_empty() {
        StepVerifier.create(accountService.account())
                .expectComplete()
                .verify();
    }

    @Test
    void credit_success() {
        var account = accountRepository.save(new Account(null, 200000)).block();

        accountService.credit(100000L)
                .doOnNext(creditedAccount -> {
                    assertAll(
                            () -> assertThat(creditedAccount).extracting(Account::getId).isEqualTo(account.getId()),
                            () -> assertThat(creditedAccount).extracting(Account::getBalance).isEqualTo(300000L)
                    );
                })
                .block();

        accountRepository.findById(account.getId())
                .doOnNext(actualAccount -> {
                    assertAll(
                            () -> assertThat(actualAccount).extracting(Account::getId).isEqualTo(account.getId()),
                            () -> assertThat(actualAccount).extracting(Account::getBalance).isEqualTo(300000L)
                    );
                })
                .block();
    }

    @Test
    void credit_notFound() {
        StepVerifier.create(accountService.credit(100000))
                .expectError(AccountNotFoundException.class)
                .verify();
    }

    @Test
    void debit_success() {
        var account = accountRepository.save(new Account(null, 200000)).block();

        accountService.debit(50000L)
                .doOnNext(creditedAccount -> {
                    assertAll(
                            () -> assertThat(creditedAccount).extracting(Account::getId).isEqualTo(account.getId()),
                            () -> assertThat(creditedAccount).extracting(Account::getBalance).isEqualTo(150000L)
                    );
                })
                .block();

        accountRepository.findById(account.getId())
                .doOnNext(actualAccount -> {
                    assertAll(
                            () -> assertThat(actualAccount).extracting(Account::getId).isEqualTo(account.getId()),
                            () -> assertThat(actualAccount).extracting(Account::getBalance).isEqualTo(150000L)
                    );
                })
                .block();
    }

    @Test
    void debit_insufficientBalance() {
        var account = accountRepository.save(new Account(null, 50000)).block();

        StepVerifier.create(accountService.debit(100000))
                .expectError(InsufficientBalanceException.class)
                .verify();
    }

    @Test
    void debit_notFound() {
        StepVerifier.create(accountService.debit(100000))
                .expectError(AccountNotFoundException.class)
                .verify();
    }

}
