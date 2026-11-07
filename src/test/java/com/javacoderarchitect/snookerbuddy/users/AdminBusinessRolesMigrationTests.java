package com.javacoderarchitect.snookerbuddy.users;

import static org.assertj.core.api.Assertions.assertThat;

import com.javacoderarchitect.snookerbuddy.BaseIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

@SqlGroup({
    @Sql("/test-data.sql"),
    @Sql(
            statements = {
                "delete from user_roles where user_id = 1 and role in ('ROLE_COACH', 'ROLE_PLAYER')",
                "insert into users(id, name, email, password) values (3, 'Other Admin', 'otheradmin@example.com', 'unused')",
                "insert into user_roles(user_id, role) values (3, 'ROLE_ADMIN')"
            }),
    @Sql("/db/migration/V006__grant_default_admin_business_roles.sql"),
    @Sql("/db/migration/V006__grant_default_admin_business_roles.sql")
})
class AdminBusinessRolesMigrationTests extends BaseIT {
    private final UserDetailsService details;

    @Autowired
    AdminBusinessRolesMigrationTests(UserDetailsService details) {
        this.details = details;
    }

    @Test
    void grantsBothRolesOnlyToTheSeededAdminAndCanBeRepeated() {
        assertThat(details.loadUserByUsername("admin@gmail.com").getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_COACH", "ROLE_PLAYER");
        assertThat(details.loadUserByUsername("otheradmin@example.com").getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactly("ROLE_ADMIN");
        assertThat(details.loadUserByUsername("demouser@gmail.com").getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactlyInAnyOrder("ROLE_COACH", "ROLE_PLAYER");
    }
}
