package com.github.dgaponov99.practicum.mymarket.app.auth;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public class IdentityUserDetails implements UserDetails {

    private final long userId;
    private final String username;
    private final String password;
    private final Set<GrantedAuthority> authorities;
    private final boolean enabled;

    public IdentityUserDetails(long userId, String username, String password, String... authorities) {
        this(userId, username, password, true, authorities);
    }

    public IdentityUserDetails(long userId, String username, String password, boolean enabled, String... authorities) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.authorities = Stream.of(authorities).map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
        this.authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        this.enabled = enabled;
    }
}
