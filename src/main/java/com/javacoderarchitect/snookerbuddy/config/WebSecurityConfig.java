package com.javacoderarchitect.snookerbuddy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
    private static final String[] PUBLIC_RESOURCES = {
        "/css/**",
        "/js/**",
        "/images/**",
        "/webjars/**",
        "/favicon.ico",
        "/actuator/health/**",
        "/actuator/info/**",
        "/error",
        "/login",
        "/registration"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        String[] unsecuredPaths = {
            "/login",
        };
        http.securityMatcher("/**");

        http.authorizeHttpRequests(r -> r.requestMatchers(PUBLIC_RESOURCES)
                .permitAll()
                .requestMatchers(unsecuredPaths)
                .permitAll()
                .requestMatchers("/exercises/**", "/players/**", "/training/new")
                .hasRole("COACH")
                .requestMatchers(HttpMethod.POST, "/training")
                .hasRole("COACH")
                .requestMatchers("/training/**", "/scores/**")
                .hasAnyRole("COACH", "PLAYER")
                .anyRequest()
                .authenticated());

        http.formLogin(formLogin -> formLogin.loginPage("/login").permitAll());

        http.logout(logout -> logout.logoutRequestMatcher(PathPatternRequestMatcher.pathPattern("/logout"))
                .logoutSuccessUrl("/")
                .permitAll());

        return http.build();
    }
}
