package com.github.dgaponov99.practicum.mymarket.payment.web.controller;

import com.github.dgaponov99.practicum.mymarket.payment.exception.AccountAlreadyExistException;
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
    public Mono<ResponseEntity<AccountDTO>> getAccount(ServerWebExchange exchange) {
        return accountService.account()
                .map(accountMapper::accountToDto)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.error(new AccountNotFoundException()));
    }

    @Override
    public Mono<ResponseEntity<AccountDTO>> createAccount(Long initialBalance, ServerWebExchange exchange) {
        return accountService.create(initialBalance)
                .map(accountMapper::accountToDto)
                .map(accountDTO -> ResponseEntity.status(HttpStatus.CREATED).body(accountDTO))
                .onErrorResume(AccountAlreadyExistException.class,
                        ex -> Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build()));
    }

    @Override
    public Mono<ResponseEntity<AccountDTO>> credit(Mono<AmountDTO> amountDTO, ServerWebExchange exchange) {
        return amountDTO
                .map(AmountDTO::getAmount)
                .flatMap(accountService::credit)
                .map(accountMapper::accountToDto)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<AccountDTO>> debit(Mono<AmountDTO> amountDTO, ServerWebExchange exchange) {
        return amountDTO
                .map(AmountDTO::getAmount)
                .flatMap(accountService::debit)
                .map(accountMapper::accountToDto)
                .map(ResponseEntity::ok)
                .onErrorResume(InsufficientBalanceException.class,
                        ex -> Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build()));
    }
}
