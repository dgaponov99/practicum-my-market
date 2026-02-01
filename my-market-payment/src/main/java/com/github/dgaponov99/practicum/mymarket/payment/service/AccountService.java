package com.github.dgaponov99.practicum.mymarket.payment.service;

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
        return accountRepository.save(new Account(null, initialBalance));
    }

    public Mono<Account> account(long id) {
        return accountRepository.findById(id);
    }

    public Mono<Account> credit(long id, long amount) {
        return account(id)
                .switchIfEmpty(Mono.error(new AccountNotFoundException()))
                .flatMap(account -> {
                    account.setBalance(account.getBalance() + amount);
                    return accountRepository.save(account);
                });
    }

    public Mono<Account> debit(long id, long amount) {
        return account(id)
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
