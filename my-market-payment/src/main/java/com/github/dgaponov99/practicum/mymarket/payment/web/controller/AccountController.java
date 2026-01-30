package com.github.dgaponov99.practicum.mymarket.payment.web.controller;

import com.github.dgaponov99.practicum.mymarket.payment.exception.AccountNotFoundException;
import com.github.dgaponov99.practicum.mymarket.payment.exception.InsufficientBalanceException;
import com.github.dgaponov99.practicum.mymarket.payment.service.AccountService;
import com.github.dgaponov99.practicum.mymarket.payment.web.dto.AccountDTO;
import com.github.dgaponov99.practicum.mymarket.payment.web.dto.AmountDTO;
import com.github.dgaponov99.practicum.mymarket.payment.web.mapper.AccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class AccountController implements AccountApi {

    private final AccountService accountService;
    private final AccountMapper accountMapper;

    @Override
    public Mono<ResponseEntity<AccountDTO>> createAccount(Long initialBalance, ServerWebExchange exchange) {
        return accountService.create(initialBalance)
                .map(accountMapper::accountToDto)
                .map(accountDTO -> ResponseEntity.status(HttpStatus.CREATED).body(accountDTO));
    }

    @Override
    public Mono<ResponseEntity<AccountDTO>> getAccount(Long accountId, ServerWebExchange exchange) {
        return accountService.account(accountId)
                .map(accountMapper::accountToDto)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.error(new AccountNotFoundException()));
    }

    @Override
    public Mono<ResponseEntity<AccountDTO>> credit(Long accountId, Mono<AmountDTO> amountDTO, ServerWebExchange exchange) {
        return amountDTO
                .map(AmountDTO::getAmount)
                .flatMap(amount -> accountService.credit(accountId, amount))
                .map(accountMapper::accountToDto)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<AccountDTO>> debit(Long accountId, Mono<AmountDTO> amountDTO, ServerWebExchange exchange) {
        return amountDTO
                .map(AmountDTO::getAmount)
                .flatMap(amount -> accountService.debit(accountId, amount))
                .map(accountMapper::accountToDto)
                .map(ResponseEntity::ok)
                .onErrorResume(InsufficientBalanceException.class,
                        ex -> Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build()));
    }
}
