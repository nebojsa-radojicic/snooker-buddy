package com.javacoderarchitect.snookerbuddy.training;

import com.javacoderarchitect.snookerbuddy.users.AuthUtils;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.stream.Collectors;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
class ScoreController {
    private static final String ASSIGNED_EXERCISE_SCORES = "/training/{planId}/exercises/{exerciseId}/scores";

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

    @GetMapping(ASSIGNED_EXERCISE_SCORES)
    String recording(@PathVariable Long planId, @PathVariable Long exerciseId, Model model) {
        model.addAttribute("result", new CreateScoreCmd(null, null));
        return recordingForm(AuthUtils.getCurrentUserIdOrThrow(), planId, exerciseId, model);
    }

    @PostMapping(path = ASSIGNED_EXERCISE_SCORES, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String submit(
            @PathVariable Long planId,
            @PathVariable Long exerciseId,
            @Valid @ModelAttribute(value = "result", binding = false) CreateScoreCmd cmd,
            BindingResult errors,
            Model model,
            RedirectAttributes redirect) {
        Long userId = AuthUtils.getCurrentUserIdOrThrow();
        if (errors.hasErrors()) return recordingForm(userId, planId, exerciseId, model);

        service.save(userId, planId, exerciseId, cmd);
        redirect.addFlashAttribute("successMessage", "Result saved.");
        return "redirect:/scores";
    }

    private String recordingForm(Long userId, Long planId, Long exerciseId, Model model) {
        var plan = service.recording(userId, planId, exerciseId);
        var exercise = plan.exercises().stream()
                .filter(item -> item.id().equals(exerciseId))
                .findFirst()
                .orElseThrow();
        model.addAttribute("plan", plan);
        model.addAttribute("exercise", exercise);
        return "training/record-result";
    }

    @PostMapping(ASSIGNED_EXERCISE_SCORES)
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidCounts(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(" "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail unreadableCounts() {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Send valid JSON with whole numbers for total and successful attempts.");
    }
}
