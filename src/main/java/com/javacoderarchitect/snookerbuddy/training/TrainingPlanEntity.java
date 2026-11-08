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

    @ElementCollection
    @CollectionTable(name = "training_plan_exercises", joinColumns = @JoinColumn(name = "plan_id"))
    @OrderColumn(name = "position")
    private List<TrainingPlanExercise> exercises = new ArrayList<>();

    protected TrainingPlanEntity() {}

    TrainingPlanEntity(
            Long coachId,
            Long playerId,
            String name,
            AssignmentFormat format,
            SessionKind kind,
            List<TrainingPlanExercise> exercises) {
        this.coachId = coachId;
        this.playerId = playerId;
        this.name = name;
        this.format = format;
        this.kind = kind;
        this.exercises = new ArrayList<>(exercises);
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

    public List<Long> getExerciseIds() {
        return exercises.stream().map(TrainingPlanExercise::getExerciseId).toList();
    }

    public List<TrainingPlanExercise> getExercises() {
        return List.copyOf(exercises);
    }
}
