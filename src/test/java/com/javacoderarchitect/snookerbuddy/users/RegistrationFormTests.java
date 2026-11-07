package com.javacoderarchitect.snookerbuddy.users;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.javacoderarchitect.snookerbuddy.ApplicationProperties;
import com.javacoderarchitect.snookerbuddy.config.WebSecurityConfig;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import(WebSecurityConfig.class)
@EnableConfigurationProperties(ApplicationProperties.class)
class RegistrationFormTests {
    private final MockMvc mvc;

    @MockitoBean
    private UserService service;

    @Autowired
    RegistrationFormTests(MockMvc mvc) {
        this.mvc = mvc;
    }

    @ParameterizedTest
    @ValueSource(strings = {"coach", "player"})
    void registersOneRoleWhenTheOtherCheckboxIsOmitted(String role) throws Exception {
        mvc.perform(post("/registration")
                        .with(csrf())
                        .param("name", "Account")
                        .param("email", "account@example.com")
                        .param("password", "password")
                        .param(role, "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        verify(service)
                .createUser(new CreateUserCmd(
                        "Account",
                        "account@example.com",
                        "password",
                        Set.of(role.equals("coach") ? Role.ROLE_COACH : Role.ROLE_PLAYER)));
    }

    @Test
    void registersBothRoles() throws Exception {
        mvc.perform(post("/registration")
                        .with(csrf())
                        .param("name", "Account")
                        .param("email", "account@example.com")
                        .param("password", "password")
                        .param("coach", "true")
                        .param("player", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        verify(service)
                .createUser(new CreateUserCmd(
                        "Account", "account@example.com", "password", Set.of(Role.ROLE_COACH, Role.ROLE_PLAYER)));
    }

    @Test
    void showsRoleValidationWhenBothCheckboxesAreOmitted() throws Exception {
        mvc.perform(post("/registration")
                        .with(csrf())
                        .param("name", "Account")
                        .param("email", "account@example.com")
                        .param("password", "password"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("user", "roleSelected"))
                .andExpect(content().string(containsString("Choose coach, player, or both")));
        verifyNoInteractions(service);
    }

    @Test
    void showsOtherFieldValidationWhenRoleCheckboxesAreOmitted() throws Exception {
        mvc.perform(post("/registration")
                        .with(csrf())
                        .param("name", "")
                        .param("email", "invalid-email")
                        .param("password", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Name is required")))
                .andExpect(content().string(containsString("Invalid email address")))
                .andExpect(content().string(containsString("Password is required")));
        verifyNoInteractions(service);
    }
}
