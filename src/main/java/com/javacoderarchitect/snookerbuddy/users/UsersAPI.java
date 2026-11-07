package com.javacoderarchitect.snookerbuddy.users;

import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
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

    public boolean hasRole(Long userId, Role role) {
        return findById(userId).map(user -> user.hasRole(role)).orElse(false);
    }

    public void requireRole(Long userId, Role role) {
        if (!hasRole(userId, role)) throw new AccessDeniedException("Access denied");
    }

    private UserDto dto(UserEntity user) {
        return new UserDto(user.getId(), user.getName(), user.getEmail(), user.getRoles(), user.getCreatedAt());
    }
}
