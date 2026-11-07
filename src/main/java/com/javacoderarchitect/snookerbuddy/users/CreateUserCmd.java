package com.javacoderarchitect.snookerbuddy.users;

import java.util.Set;

record CreateUserCmd(String name, String email, String password, Set<Role> roles) {
    CreateUserCmd {
        roles = Set.copyOf(roles);
        if (roles.isEmpty() || !Set.of(Role.ROLE_COACH, Role.ROLE_PLAYER).containsAll(roles)) {
            throw new IllegalArgumentException("Choose coach, player, or both.");
        }
    }
}
