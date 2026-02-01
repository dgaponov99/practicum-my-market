package com.github.dgaponov99.practicum.mymarket.payment.module.web;

import com.github.dgaponov99.practicum.mymarket.payment.config.MapstructConfiguration;
import com.github.dgaponov99.practicum.mymarket.payment.config.SecurityConfiguration;
import com.github.dgaponov99.practicum.mymarket.payment.exception.AccountNotFoundException;
import com.github.dgaponov99.practicum.mymarket.payment.exception.InsufficientBalanceException;
import com.github.dgaponov99.practicum.mymarket.payment.persistence.entity.Account;
import com.github.dgaponov99.practicum.mymarket.payment.service.AccountService;
import com.github.dgaponov99.practicum.mymarket.payment.web.controller.AccountController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;

@WebFluxTest(controllers = AccountController.class)
@Import({MapstructConfiguration.class, SecurityConfiguration.class})
@ComponentScan(basePackages = "com.github.dgaponov99.practicum.mymarket.payment.web")
public class AccountControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    AccountService accountService;

    @Test
    @WithMockUser(roles = "PAYMENT")
    void getAccount_success() {
        when(accountService.account(anyLong())).thenReturn(Mono.just(new Account(1L, 10000L)));

        webTestClient.get()
                .uri("/account/{accountId}", 1L)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isEqualTo(10000L);

        verify(accountService, times(1)).account(1L);
    }

    @Test
    void getAccount_unauthorized() {
        webTestClient.get()
                .uri("/account/{accountId}", 1L)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isUnauthorized();

        verify(accountService, never()).account(anyLong());
    }

    @Test
    @WithMockUser
    void getAccount_forbidden() {
        webTestClient.get()
                .uri("/account/{accountId}", 1L)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isForbidden();

        verify(accountService, never()).account(anyLong());
    }

    @Test
    @WithMockUser(roles = "PAYMENT")
    void getAccount_notFound() {
        when(accountService.account(anyLong())).thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/account/{accountId}", 100500L)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();

        verify(accountService, times(1)).account(100500L);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "0", "10000", "null"
    }, nullValues = "null")
    @WithMockUser(roles = "PAYMENT")
    void createAccount_success(Long initialBalance) {
        var expectBalance = initialBalance == null ? 0L : initialBalance;
        when(accountService.create(anyLong())).thenReturn(Mono.just(new Account(1L, expectBalance)));

        webTestClient.post()
                .uri(uriBuilder -> {
                    uriBuilder.path("/account");
                    if (initialBalance != null) {
                        uriBuilder.queryParam("initialBalance", initialBalance);
                    }
                    return uriBuilder.build();
                })
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.balance").isEqualTo(expectBalance);

        verify(accountService, times(1)).create(expectBalance);
    }

    @Test
    @WithMockUser(roles = "PAYMENT")
    void createAccount_notValid() {
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/account")
                        .queryParam("initialBalance", -1L)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors").isNotEmpty();

        verify(accountService, never()).create(anyLong());
    }

    @Test
    @WithMockUser(roles = "PAYMENT")
    void credit_success() {
        when(accountService.credit(anyLong(), anyLong())).thenReturn(Mono.just(new Account(1L, 150000L)));
        webTestClient.post()
                .uri("/account/{accountId}/credit", 1L)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        { "amount": %d }
                        """.formatted(50000L))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isEqualTo(150000L);

        verify(accountService, times(1)).credit(1L, 50000L);
    }

    @Test
    @WithMockUser(roles = "PAYMENT")
    void credit_notFound() {
        when(accountService.credit(anyLong(), anyLong())).thenReturn(Mono.error(new AccountNotFoundException()));

        webTestClient.post()
                .uri("/account/{accountId}/credit", 100500L)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        { "amount": %d }
                        """.formatted(50000L))
                .exchange()
                .expectStatus().isNotFound();

        verify(accountService, times(1)).credit(100500L, 50000L);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "0", "-100", "null"
    }, nullValues = "null")
    @WithMockUser(roles = "PAYMENT")
    void credit_notValid(Long amount) {
        var jsonBody = amount == null ? "{}" : """
                { "amount": %d }
                """.formatted(amount);

        webTestClient.post()
                .uri("/account/{accountId}/credit", 1L)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(jsonBody)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors").isNotEmpty();

        verify(accountService, never()).credit(anyLong(), anyLong());
    }

    @Test
    @WithMockUser(roles = "PAYMENT")
    void debit_success() {
        when(accountService.debit(anyLong(), anyLong())).thenReturn(Mono.just(new Account(1L, 150000L)));
        webTestClient.post()
                .uri("/account/{accountId}/debit", 1L)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        { "amount": %d }
                        """.formatted(50000L))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isEqualTo(150000L);

        verify(accountService, times(1)).debit(1L, 50000L);
    }

    @Test
    @WithMockUser(roles = "PAYMENT")
    void debit_notFound() {
        when(accountService.debit(anyLong(), anyLong())).thenReturn(Mono.error(new AccountNotFoundException()));

        webTestClient.post()
                .uri("/account/{accountId}/debit", 1L)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        { "amount": %d }
                        """.formatted(50000L))
                .exchange()
                .expectStatus().isNotFound();

        verify(accountService, times(1)).debit(1L, 50000L);
    }

    @Test
    @WithMockUser(roles = "PAYMENT")
    void debit_insufficientBalance() {
        when(accountService.debit(anyLong(), anyLong())).thenReturn(Mono.error(new InsufficientBalanceException()));

        webTestClient.post()
                .uri("/account/{accountId}/debit", 1L)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        { "amount": %d }
                        """.formatted(50000L))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);

        verify(accountService, times(1)).debit(1L, 50000L);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "0", "-100", "null"
    }, nullValues = "null")
    @WithMockUser(roles = "PAYMENT")
    void debit_notValid(Long amount) {
        var jsonBody = amount == null ? "{}" : """
                { "amount": %d }
                """.formatted(amount);

        webTestClient.post()
                .uri("/account/{accountId}/debit", 1L)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(jsonBody)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors").isNotEmpty();

        verify(accountService, never()).debit(anyLong(), anyLong());
    }

}
