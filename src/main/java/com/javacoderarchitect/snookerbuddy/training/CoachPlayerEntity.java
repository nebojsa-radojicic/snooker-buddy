package com.javacoderarchitect.snookerbuddy.training;

import com.javacoderarchitect.snookerbuddy.shared.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "coach_players")
class CoachPlayerEntity extends BaseEntity {
    @Id
    private Long playerId;

    @Column(nullable = false)
    private Long coachId;

    protected CoachPlayerEntity() {}

    CoachPlayerEntity(Long coachId, Long playerId) {
        this.coachId = coachId;
        this.playerId = playerId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public Long getCoachId() {
        return coachId;
    }
}
