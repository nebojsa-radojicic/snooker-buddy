package com.javacoderarchitect.snookerbuddy.training;

import com.javacoderarchitect.snookerbuddy.exercises.ExercisesAPI;
import com.javacoderarchitect.snookerbuddy.users.AuthUtils;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/training")
class TrainingController {
    private final TrainingService service;
    private final PlayerService players;
    private final ExercisesAPI exercises;

    TrainingController(TrainingService service, PlayerService players, ExercisesAPI exercises) {
        this.service = service;
        this.players = players;
        this.exercises = exercises;
    }

    @GetMapping
    String list(Model model) {
        model.addAttribute("plans", service.plans(AuthUtils.getCurrentUserIdOrThrow()));
        model.addAttribute("currentUserId", AuthUtils.getCurrentUserIdOrThrow());
        return "training/list";
    }

    @GetMapping("/new")
    String form(@RequestParam(name = "exercise", required = false) List<Long> ids, Model model) {
        var selected = ids == null ? List.<Long>of() : ids.stream().distinct().toList();
        exercises.selected(AuthUtils.getCurrentUserIdOrThrow(), selected);
        model.addAttribute(
                "assignment",
                new CreateAssignmentCmd(
                        null,
                        "",
                        selected.size() == 1
                                ? AssignmentFormat.INDIVIDUAL_EXERCISES
                                : AssignmentFormat.TRAINING_SESSION,
                        SessionKind.INDEPENDENT,
                        selected,
                        false,
                        10,
                        8));
        choices(model);
        return "training/form";
    }

    @PostMapping
    String assign(
            @Valid @ModelAttribute("assignment") CreateAssignmentCmd cmd,
            BindingResult errors,
            Model model,
            RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                var ids = service.assign(AuthUtils.getCurrentUserIdOrThrow(), cmd);
                redirect.addFlashAttribute(
                        "successMessage",
                        ids.size() == 1
                                ? "Training assigned successfully."
                                : ids.size() + " exercises assigned individually.");
                return ids.size() == 1 ? "redirect:/training/" + ids.getFirst() : "redirect:/training";
            } catch (IllegalArgumentException ex) {
                errors.reject("assignment.invalid", ex.getMessage());
            }
        }
        choices(model);
        return "training/form";
    }

    private void choices(Model model) {
        Long userId = AuthUtils.getCurrentUserIdOrThrow();
        model.addAttribute("players", players.players(userId));
        model.addAttribute("exercises", exercises.library(userId));
        model.addAttribute("formats", AssignmentFormat.values());
        model.addAttribute("kinds", SessionKind.values());
    }

    @GetMapping("/{id}")
    String detail(@PathVariable Long id, Model model) {
        model.addAttribute("plan", service.plan(AuthUtils.getCurrentUserIdOrThrow(), id));
        return "training/detail";
    }

    @GetMapping("/{id}/exercises/{exerciseId}")
    String exercise(@PathVariable Long id, @PathVariable Long exerciseId, Model model) {
        Long userId = AuthUtils.getCurrentUserIdOrThrow();
        model.addAttribute("exercise", service.exercise(userId, id, exerciseId));
        model.addAttribute("plan", service.plan(userId, id));
        return "training/exercise";
    }

    @GetMapping("/{id}/exercises/{exerciseId}/drawing")
    @ResponseBody
    ResponseEntity<byte[]> drawing(@PathVariable Long id, @PathVariable Long exerciseId) {
        var drawing = service.drawing(AuthUtils.getCurrentUserIdOrThrow(), id, exerciseId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(drawing.contentType()))
                .header("X-Content-Type-Options", "nosniff")
                .body(drawing.bytes());
    }

    @ExceptionHandler(ResponseStatusException.class)
    ModelAndView missing(ResponseStatusException ex) {
        var view = new ModelAndView("error/404");
        view.setStatus(ex.getStatusCode());
        view.addObject("message", ex.getReason());
        return view;
    }
}
