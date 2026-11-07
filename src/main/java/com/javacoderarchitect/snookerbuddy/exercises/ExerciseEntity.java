package com.javacoderarchitect.snookerbuddy.exercises;

import com.javacoderarchitect.snookerbuddy.shared.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "exercises")
class ExerciseEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "exercise_id_generator")
    @SequenceGenerator(name = "exercise_id_generator", sequenceName = "exercise_id_seq")
    private Long id;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false, length = 5000)
    private String description;

    @Column(nullable = false, length = 2000)
    private String goal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExerciseType type;

    @Column(nullable = false)
    private byte[] drawing;

    @Column(nullable = false)
    private String drawingContentType;

    protected ExerciseEntity() {}

    ExerciseEntity(Long ownerId, CreateExerciseCmd cmd, byte[] drawing, String contentType) {
        this.ownerId = ownerId;
        this.name = cmd.name().strip();
        this.description = cmd.description().strip();
        this.goal = cmd.goal().strip();
        this.difficulty = cmd.difficulty();
        this.type = cmd.type();
        this.drawing = drawing;
        this.drawingContentType = contentType;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getGoal() {
        return goal;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public ExerciseType getType() {
        return type;
    }

    public byte[] getDrawing() {
        return drawing;
    }

    public String getDrawingContentType() {
        return drawingContentType;
    }
}
