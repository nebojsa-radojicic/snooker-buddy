package com.javacoderarchitect.snookerbuddy.exercises;

import com.javacoderarchitect.snookerbuddy.users.Role;
import com.javacoderarchitect.snookerbuddy.users.UsersAPI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
@Transactional(readOnly = true)
public class ExercisesAPI {
    private final ExerciseService service;
    private final ExerciseRepository repository;

    private final UsersAPI users;

    ExercisesAPI(ExerciseService service, ExerciseRepository repository, UsersAPI users) {
        this.users = users;
        this.service = service;
        this.repository = repository;
    }

    public List<ExerciseDto> library(Long ownerId) {
        return service.findLibrary(ownerId, "", null, null);
    }

    public List<ExerciseDto> selected(Long ownerId, List<Long> ids) {
        users.requireRole(ownerId, Role.ROLE_COACH);
        if (ids.isEmpty()) return List.of();
        var found = repository.findOwned(ownerId, ids).stream()
                .collect(java.util.stream.Collectors.toMap(ExerciseDto::id, e -> e));
        if (found.size() != ids.size()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercise not found");
        return ids.stream().map(found::get).toList();
    }

    public ExerciseDrawingDto drawing(Long ownerId, Long id) {
        var drawing = service.drawing(ownerId, id);
        return new ExerciseDrawingDto(drawing.bytes(), drawing.contentType());
    }
}
