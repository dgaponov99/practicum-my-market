package com.github.dgaponov99.practicum.mymarket.app.web.advice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ServerWebExchange;

@Slf4j
@ControllerAdvice
public class WebExceptionHandler {

    @ExceptionHandler(Exception.class)
    public void exception(Exception e, ServerWebExchange exchange) {
        log.error("uri: {}\nmessage: {}", exchange.getRequest().getURI(), e.getMessage(), e);
    }

}
