package com.javacoderarchitect.snookerbuddy.users;

import java.time.Instant;

public record UserDto(Long id, String name, String email, Role role, Instant createdAt) {}
