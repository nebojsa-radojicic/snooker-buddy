package com.javacoderarchitect.snookerbuddy.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.javacoderarchitect.snookerbuddy.BaseIT;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@Sql("/test-data.sql")
class UserControllerTests extends BaseIT {
    @Autowired
    private UserRepository users;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService details;

    @Test
    void registersBothRolesAndLoadsBothAuthoritiesWithoutAdminEscalation() throws Exception {
        mockMvc.perform(post("/registration")
                        .with(csrf())
                        .param("name", "Dual Role")
                        .param("email", "dual@example.com")
                        .param("password", "password")
                        .param("coach", "true")
                        .param("player", "true")
                        .param("role", "ROLE_ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        assertThat(users.findByEmailIgnoreCase("dual@example.com").orElseThrow().getRoles())
                .isEqualTo(Set.of(Role.ROLE_COACH, Role.ROLE_PLAYER));
        assertThat(details.loadUserByUsername("dual@example.com").getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactlyInAnyOrder("ROLE_COACH", "ROLE_PLAYER");
    }

    @Test
    void requiresAtLeastOneBusinessRole() throws Exception {
        mockMvc.perform(post("/registration")
                        .with(csrf())
                        .param("name", "No Role")
                        .param("email", "none@example.com")
                        .param("password", "password"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("user", "roleSelected"));
        assertThat(users.findByEmailIgnoreCase("none@example.com")).isEmpty();
    }

    @Test
    void shouldRegisterUserSuccessfully() {
        var result = mvc.post()
                .uri("/registration")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("coach", "true")
                .param("name", "testuser123")
                .param("email", "testuser123@gmail.com")
                .param("password", "testuser123");

        assertThat(result).hasStatus(HttpStatus.FOUND);
    }
}
