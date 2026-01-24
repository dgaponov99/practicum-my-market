package com.github.dgaponov99.practicum.mymarket.payment.web.advice;

import com.github.dgaponov99.practicum.mymarket.payment.exception.AccountNotFoundException;
import com.github.dgaponov99.practicum.mymarket.payment.web.dto.ErrorDTO;
import com.github.dgaponov99.practicum.mymarket.payment.web.dto.ErrorsDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<Void> notFound() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorsDTO> handleValidationException(WebExchangeBindException exception) {
        return ResponseEntity.badRequest()
                .body(new ErrorsDTO(exception.getFieldErrors().stream()
                        .map(DefaultMessageSourceResolvable::getDefaultMessage)
                        .toList()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorsDTO> handleException(ConstraintViolationException exception) {
        return ResponseEntity.badRequest()
                .body(new ErrorsDTO(exception.getConstraintViolations().stream()
                        .map(ConstraintViolation::getMessage)
                        .toList()));
    }

    @ExceptionHandler(ErrorResponseException.class)
    public Mono<Void> errorResponseException(ErrorResponseException exception) {
        return Mono.error(exception);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorDTO> unexpectedException(Throwable ex) {
        log.error(ex.getMessage(), ex);
        return ResponseEntity.internalServerError().body(new ErrorDTO(ex.getMessage()));
    }

}
