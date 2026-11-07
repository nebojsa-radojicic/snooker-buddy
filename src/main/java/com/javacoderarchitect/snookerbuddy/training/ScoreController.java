package com.javacoderarchitect.snookerbuddy.training;

import com.javacoderarchitect.snookerbuddy.users.AuthUtils;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

@Controller
class ScoreController {
    private final ScoreService service;

    ScoreController(ScoreService service) {
        this.service = service;
    }

    @GetMapping("/scores")
    String scores(Model model) {
        model.addAttribute("scores", service.scores(AuthUtils.getCurrentUserIdOrThrow()));
        return "training/scores";
    }

    @GetMapping("/scores/{id}")
    @ResponseBody
    ResponseEntity<ScoreDto> score(@PathVariable Long id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.score(AuthUtils.getCurrentUserIdOrThrow(), id));
    }

    @PostMapping("/training/{planId}/exercises/{exerciseId}/scores")
    @ResponseBody
    ResponseEntity<ScoreDto> save(
            @PathVariable Long planId, @PathVariable Long exerciseId, @Valid @RequestBody CreateScoreCmd cmd) {
        var score = service.save(AuthUtils.getCurrentUserIdOrThrow(), planId, exerciseId, cmd);
        return ResponseEntity.created(URI.create("/scores/" + score.id()))
                .cacheControl(CacheControl.noStore())
                .body(score);
    }

    @ExceptionHandler(ResponseStatusException.class)
    ModelAndView missing(ResponseStatusException ex) {
        var view = new ModelAndView("error/404");
        view.setStatus(ex.getStatusCode());
        view.addObject("message", ex.getReason());
        return view;
    }

    @ExceptionHandler({
        org.springframework.web.bind.MethodArgumentNotValidException.class,
        org.springframework.http.converter.HttpMessageNotReadableException.class
    })
    ResponseEntity<Void> invalid() {
        return ResponseEntity.badRequest().build();
    }
}
