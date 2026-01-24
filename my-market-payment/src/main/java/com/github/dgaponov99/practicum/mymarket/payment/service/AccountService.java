package com.github.dgaponov99.practicum.mymarket.payment.service;

import com.github.dgaponov99.practicum.mymarket.payment.exception.AccountAlreadyExistException;
import com.github.dgaponov99.practicum.mymarket.payment.exception.AccountNotFoundException;
import com.github.dgaponov99.practicum.mymarket.payment.exception.InsufficientBalanceException;
import com.github.dgaponov99.practicum.mymarket.payment.persistence.entity.Account;
import com.github.dgaponov99.practicum.mymarket.payment.persistence.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public Mono<Account> create(long initialBalance) {
        return account()
                .hasElement()
                .flatMap(exist -> exist
                        ? Mono.error(new AccountAlreadyExistException())
                        : accountRepository.save(new Account(null, initialBalance))
                );
    }

    public Mono<Account> account() {
        return accountRepository.findAll().next();
    }

    public Mono<Account> credit(long amount) {
        return account()
                .switchIfEmpty(Mono.error(new AccountNotFoundException()))
                .flatMap(account -> {
                    account.setBalance(account.getBalance() + amount);
                    return accountRepository.save(account);
                });
    }

    public Mono<Account> debit(long amount) {
        return account()
                .switchIfEmpty(Mono.error(new AccountNotFoundException()))
                .flatMap(account -> {
                    if (account.getBalance() < amount) {
                        return Mono.error(new InsufficientBalanceException());
                    }
                    account.setBalance(account.getBalance() - amount);
                    return accountRepository.save(account);
                });
    }

}
