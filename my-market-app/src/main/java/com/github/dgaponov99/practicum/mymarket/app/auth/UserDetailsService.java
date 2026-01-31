package com.github.dgaponov99.practicum.mymarket.app.auth;

import com.github.dgaponov99.practicum.mymarket.app.percistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserDetailsService implements ReactiveUserDetailsService {

    private final UserRepository userRepository;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(user -> new IdentityUserDetails(user.getId(), user.getUsername(), user.getPassword(), !user.isDisabled()));
    }

}
