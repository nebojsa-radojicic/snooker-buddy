package com.javacoderarchitect.snookerbuddy.users;

import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class UsersAPI {
    private final UserRepository repository;

    UsersAPI(UserRepository repository) {
        this.repository = repository;
    }

    public Optional<UserDto> findByEmail(String email) {
        return repository.findByEmailIgnoreCase(email.strip()).map(this::dto);
    }

    public Optional<UserDto> findById(Long id) {
        return repository.findById(id).map(this::dto);
    }

    private UserDto dto(UserEntity user) {
        return new UserDto(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
