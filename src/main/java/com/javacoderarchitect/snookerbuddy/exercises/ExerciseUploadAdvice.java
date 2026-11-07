package com.javacoderarchitect.snookerbuddy.exercises;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

// Multipart parsing can fail before a controller is selected.
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class ExerciseUploadAdvice {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ModelAndView oversized() {
        var view = new ModelAndView("exercises/form");
        view.setStatus(HttpStatus.CONTENT_TOO_LARGE);
        view.addObject("exercise", new CreateExerciseCmd("", "", "", Difficulty.EASY, ExerciseType.TECHNIQUE, null));
        view.addObject("types", ExerciseType.values());
        view.addObject("difficulties", Difficulty.values());
        view.addObject("errorMessage", "Choose a photo up to 20 MB, then submit your exercise again.");
        return view;
    }
}
