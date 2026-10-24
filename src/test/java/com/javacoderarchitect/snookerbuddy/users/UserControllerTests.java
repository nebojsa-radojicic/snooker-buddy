package com.javacoderarchitect.snookerbuddy.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.javacoderarchitect.snookerbuddy.BaseIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;

@Sql("/test-data.sql")
class UserControllerTests extends BaseIT {

    @Test
    void shouldRegisterUserSuccessfully() {
        var result = mvc.post()
                .uri("/registration")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "testuser123")
                .param("email", "testuser123@gmail.com")
                .param("password", "testuser123");

        assertThat(result).hasStatus(HttpStatus.FOUND);
    }
}
