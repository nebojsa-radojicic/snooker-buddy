package com.javacoderarchitect.snookerbuddy.exercises;

import com.javacoderarchitect.snookerbuddy.users.Role;
import com.javacoderarchitect.snookerbuddy.users.UsersAPI;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class ExerciseService {
    private final ExerciseRepository repository;

    private final UsersAPI users;

    ExerciseService(ExerciseRepository repository, UsersAPI users) {
        this.repository = repository;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<ExerciseDto> findLibrary(Long ownerId, String query, Difficulty difficulty, ExerciseType type) {
        users.requireRole(ownerId, Role.ROLE_COACH);
        String search = query.strip().toLowerCase(java.util.Locale.ROOT);
        return repository.findLibrary(ownerId, search, difficulty, type);
    }

    @Transactional
    public Long create(Long ownerId, CreateExerciseCmd cmd) {
        users.requireRole(ownerId, Role.ROLE_COACH);
        var file = cmd.drawing();
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Upload your exercise drawing.");
        if (file.getSize() > DrawingService.MAX_BYTES)
            throw new IllegalArgumentException("Choose a photo up to 20 MB.");
        try {
            var drawing = DrawingService.prepare(file.getBytes());
            return repository
                    .save(new ExerciseEntity(ownerId, cmd, drawing.bytes(), drawing.contentType()))
                    .getId();
        } catch (IOException ex) {
            throw new IllegalArgumentException("This photo could not be read. Please choose another.", ex);
        }
    }

    @Transactional(readOnly = true)
    public ExerciseDto find(Long ownerId, Long id) {
        return ExerciseDto.from(owned(ownerId, id));
    }

    @Transactional(readOnly = true)
    public DrawingService.Drawing drawing(Long ownerId, Long id) {
        var exercise = owned(ownerId, id);
        return new DrawingService.Drawing(exercise.getDrawing(), exercise.getDrawingContentType());
    }

    private ExerciseEntity owned(Long ownerId, Long id) {
        users.requireRole(ownerId, Role.ROLE_COACH);
        return repository
                .findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercise not found"));
    }
}
