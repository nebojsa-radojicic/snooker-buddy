package com.javacoderarchitect.snookerbuddy.users;

import java.util.Collection;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class SecurityUser implements UserDetails {

    private final Long id;
    private final String name;
    private final String email;
    private final String password;
    private final Set<Role> roles;

    public SecurityUser(Long id, String name, String email, String password, Role... roles) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.roles = Set.of(roles);
    }

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .toList();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public @NonNull String getUsername() {
        return email;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<Role> getRoles() {
        return roles;
    }
}
