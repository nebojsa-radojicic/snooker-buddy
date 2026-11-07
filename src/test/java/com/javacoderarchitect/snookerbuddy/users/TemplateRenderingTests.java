package com.javacoderarchitect.snookerbuddy.users;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.javacoderarchitect.snookerbuddy.BaseIT;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class TemplateRenderingTests extends BaseIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRenderLoginWithSecurityFieldsAndFeedback() throws Exception {
        mockMvc.perform(get("/login").param("error", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Invalid email or password.")))
                .andExpect(content().string(containsString("name=\"username\"")))
                .andExpect(content().string(containsString("name=\"password\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("/css/styles.css")))
                .andExpect(content().string(not(containsString("id=\"user-menu-button\""))));
    }

    @Test
    void shouldRenderRegistrationAndFieldValidation() throws Exception {
        mockMvc.perform(get("/registration"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"registrationForm\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));

        mockMvc.perform(post("/registration")
                        .with(csrf())
                        .param("name", "")
                        .param("email", "invalid-email")
                        .param("password", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Name is required")))
                .andExpect(content().string(containsString("Invalid email address")))
                .andExpect(content().string(containsString("Password is required")))
                .andExpect(content().string(containsString("aria-invalid=\"true\"")))
                .andExpect(content().string(containsString("aria-describedby=\"email-error\"")));
    }

    @Test
    void shouldRenderUserNameAndLogoutInTheSharedLayout() throws Exception {
        var principal = new SecurityUser(1L, "Alex Coach", "alex@example.com", "unused", Role.ROLE_USER);
        mockMvc.perform(get("/").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Alex Coach")))
                .andExpect(content().string(not(containsString("alex@example.com"))))
                .andExpect(content().string(containsString("id=\"user-menu-button\"")))
                .andExpect(content().string(containsString("action=\"/logout\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("Welcome to Snooker Buddy.")));
    }

    @Test
    void shouldRenderSharedFlashMessages() throws Exception {
        mockMvc.perform(get("/login")
                        .flashAttr("successMessage", "Registration complete.")
                        .flashAttr("errorMessage", "Please try again."))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Registration complete.")))
                .andExpect(content().string(containsString("Please try again.")));
    }

    @ParameterizedTest
    @ValueSource(ints = {403, 404, 500})
    void shouldRenderErrorPagesThroughTheSharedLayout(int errorCode) throws Exception {
        mockMvc.perform(get("/error")
                        .accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, errorCode))
                .andExpect(status().is(errorCode))
                .andExpect(content().string(containsString("class=\"error-code\">" + errorCode)))
                .andExpect(content().string(containsString("Back to workspace")))
                .andExpect(content().string(containsString("/css/styles.css")))
                .andExpect(content().string(containsString("/js/layout.js")));
    }
}
