package com.github.dgaponov99.practicum.mymarket.app.config;

import com.github.dgaponov99.practicum.mymarket.app.percistence.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.WebSessionServerCsrfTokenRepository;

import java.net.URI;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    @Bean
    SecurityWebFilterChain security(ServerHttpSecurity http,
                                    RedirectServerLogoutSuccessHandler redirectServerLogoutSuccessHandler) {
        return http
                .authorizeExchange(exchangeSpec -> exchangeSpec
                        .pathMatchers(HttpMethod.GET, "/", "/login", "/items", "/items/*", "/images/*").permitAll()
                        .anyExchange().hasRole("USER"))
                .csrf(csrfSpec -> csrfSpec.csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse()))
                // Настраиваем форму логина
                .formLogin(form -> form
                        // URL страницы логина
                        .loginPage("/login")
                        .authenticationSuccessHandler(
                                // В случае успешного логина, перенаправляем на /
                                new RedirectServerAuthenticationSuccessHandler("/")
                        )
                )
                // Настраиваем обработку при выходе
                .logout(logout -> logout
                        // URL страницы выхода
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(redirectServerLogoutSuccessHandler)
                )
                .oauth2Client(Customizer.withDefaults())
                .build();
    }

    // Защищаем пароли шифрованием
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebSessionServerCsrfTokenRepository csrfTokenRepository() {
        return new WebSessionServerCsrfTokenRepository();
    }

    @Bean
    public ReactiveUserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> userRepository.findByUsername(username)
                .map(user -> User.builder()
                        .username(user.getUsername())
                        .password(user.getPassword())
                        .roles("USER")
                        .disabled(user.isDisabled())
                        .build());
    }

    // Настраиваем поведение при выходе
    @Bean
    public RedirectServerLogoutSuccessHandler redirectServerLogoutSuccessHandler() {
        RedirectServerLogoutSuccessHandler logoutSuccessHandler = new RedirectServerLogoutSuccessHandler();
        // При выходе перенаправляем его на домашнюю страницу
        logoutSuccessHandler.setLogoutSuccessUrl(URI.create("/"));
        return logoutSuccessHandler;
    }

}
