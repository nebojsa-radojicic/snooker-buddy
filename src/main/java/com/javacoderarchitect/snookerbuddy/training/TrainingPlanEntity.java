package com.javacoderarchitect.snookerbuddy.training;

import com.javacoderarchitect.snookerbuddy.shared.BaseEntity;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "training_plans")
class TrainingPlanEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "training_plan_id_generator")
    @SequenceGenerator(name = "training_plan_id_generator", sequenceName = "training_plan_id_seq")
    private Long id;

    @Column(nullable = false)
    private Long coachId;

    @Column(nullable = false)
    private Long playerId;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentFormat format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionKind kind;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private int target;

    @ElementCollection
    @CollectionTable(name = "training_plan_exercises", joinColumns = @JoinColumn(name = "plan_id"))
    @OrderColumn(name = "position")
    @Column(name = "exercise_id", nullable = false)
    private List<Long> exerciseIds = new ArrayList<>();

    protected TrainingPlanEntity() {}

    TrainingPlanEntity(
            Long coachId,
            Long playerId,
            String name,
            AssignmentFormat format,
            SessionKind kind,
            int attempts,
            int target,
            List<Long> ids) {
        this.coachId = coachId;
        this.playerId = playerId;
        this.name = name;
        this.format = format;
        this.kind = kind;
        this.attempts = attempts;
        this.target = target;
        this.exerciseIds = new ArrayList<>(ids);
    }

    public Long getId() {
        return id;
    }

    public Long getCoachId() {
        return coachId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public String getName() {
        return name;
    }

    public AssignmentFormat getFormat() {
        return format;
    }

    public SessionKind getKind() {
        return kind;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getTarget() {
        return target;
    }

    public List<Long> getExerciseIds() {
        return List.copyOf(exerciseIds);
    }
}
