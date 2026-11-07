package com.javacoderarchitect.snookerbuddy.users;

import static org.assertj.core.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

class AccountRolesTests {
    @Test
    void supportsBothBusinessRolesAsIndependentAuthorities() {
        var user = new SecurityUser(1L, "Dual", "dual@example.com", "unused", Role.ROLE_COACH, Role.ROLE_PLAYER);
        assertThat(user.getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactlyInAnyOrder("ROLE_COACH", "ROLE_PLAYER");
    }

    @Test
    void doesNotTurnLegacyOrAdminAuthoritiesIntoBusinessRoles() {
        for (var role : Set.of(Role.ROLE_USER, Role.ROLE_ADMIN)) {
            var user = new SecurityUser(1L, "Account", "account@example.com", "unused", role);
            assertThat(user.getAuthorities()).extracting(a -> a.getAuthority()).containsExactly(role.name());
            assertThatThrownBy(() -> new CreateUserCmd("Name", "email@example.com", "password", Set.of(role)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void registrationRequiresBusinessRolesAndDoesNotPermitAdminEscalation() {
        assertThatThrownBy(() -> new CreateUserCmd("Name", "email@example.com", "password", Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreateUserCmd(
                        "Name", "email@example.com", "password", Set.of(Role.ROLE_COACH, Role.ROLE_ADMIN)))
                .isInstanceOf(IllegalArgumentException.class);
        var cmd = new CreateUserCmd("Name", "email@example.com", "password", Set.of(Role.ROLE_COACH, Role.ROLE_PLAYER));
        assertThat(cmd.roles()).containsExactlyInAnyOrder(Role.ROLE_COACH, Role.ROLE_PLAYER);
    }
}
