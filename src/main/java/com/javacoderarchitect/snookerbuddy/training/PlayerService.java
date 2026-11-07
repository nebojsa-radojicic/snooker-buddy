package com.javacoderarchitect.snookerbuddy.training;

import com.javacoderarchitect.snookerbuddy.users.Role;
import com.javacoderarchitect.snookerbuddy.users.UserDto;
import com.javacoderarchitect.snookerbuddy.users.UsersAPI;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class PlayerService {
    private final CoachPlayerRepository repository;
    private final UsersAPI users;

    PlayerService(CoachPlayerRepository repository, UsersAPI users) {
        this.repository = repository;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<UserDto> players(Long coachId) {
        users.requireRole(coachId, Role.ROLE_COACH);
        return repository.findByCoachIdOrderByCreatedAtAsc(coachId).stream()
                .map(link -> users.findById(link.getPlayerId()).orElseThrow())
                .toList();
    }

    @Transactional
    public void add(Long coachId, String email) {
        users.requireRole(coachId, Role.ROLE_COACH);
        var player = users.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No account found. Ask the player to register first."));
        if (!player.hasRole(Role.ROLE_PLAYER))
            throw new IllegalArgumentException("This account does not have the player role.");
        if (player.id().equals(coachId)) throw new IllegalArgumentException("Choose a player other than yourself.");
        var existing = repository.findById(player.id());
        if (existing.isPresent()) {
            if (existing.get().getCoachId().equals(coachId))
                throw new IllegalArgumentException("This player is already in your list.");
            throw new IllegalArgumentException("This player already has a coach.");
        }
        repository.save(new CoachPlayerEntity(coachId, player.id()));
    }

    @Transactional(readOnly = true)
    public boolean isPlayer(Long coachId, Long playerId) {
        return repository.existsByCoachIdAndPlayerId(coachId, playerId);
    }
}
