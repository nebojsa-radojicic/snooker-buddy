package com.javacoderarchitect.snookerbuddy.users;

import java.time.Instant;
import java.util.Set;

public record UserDto(Long id, String name, String email, Set<Role> roles, Instant createdAt) {
    public UserDto {
        roles = Set.copyOf(roles);
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }
}
