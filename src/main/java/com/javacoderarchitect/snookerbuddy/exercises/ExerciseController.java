package com.javacoderarchitect.snookerbuddy.exercises;

import com.javacoderarchitect.snookerbuddy.users.AuthUtils;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/exercises")
class ExerciseController {
    private final ExerciseService service;

    ExerciseController(ExerciseService service) {
        this.service = service;
    }

    @ModelAttribute("difficulties")
    Difficulty[] difficulties() {
        return Difficulty.values();
    }

    @ModelAttribute("types")
    ExerciseType[] types() {
        return ExerciseType.values();
    }

    @GetMapping
    String library(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) ExerciseType type,
            Model model) {
        model.addAttribute("exercises", service.findLibrary(AuthUtils.getCurrentUserIdOrThrow(), q, difficulty, type));
        model.addAttribute("q", q);
        model.addAttribute("difficulty", difficulty);
        model.addAttribute("type", type);
        return "exercises/library";
    }

    @GetMapping("/new")
    String newExercise(Model model) {
        model.addAttribute(
                "exercise", new CreateExerciseCmd("", "", "", Difficulty.EASY, ExerciseType.TECHNIQUE, null));
        return "exercises/form";
    }

    @PostMapping
    String save(
            @Valid @ModelAttribute("exercise") CreateExerciseCmd cmd,
            BindingResult errors,
            RedirectAttributes redirect) {
        if (errors.hasErrors()) return "exercises/form";
        try {
            Long id = service.create(AuthUtils.getCurrentUserIdOrThrow(), cmd);
            redirect.addFlashAttribute("successMessage", "Exercise saved to your library.");
            return "redirect:/exercises/" + id;
        } catch (IllegalArgumentException ex) {
            errors.rejectValue("drawing", "drawing.invalid", ex.getMessage());
            return "exercises/form";
        }
    }

    @GetMapping("/{id}")
    String detail(@PathVariable Long id, Model model) {
        model.addAttribute("exercise", service.find(AuthUtils.getCurrentUserIdOrThrow(), id));
        return "exercises/detail";
    }

    @GetMapping("/{id}/drawing")
    @ResponseBody
    ResponseEntity<byte[]> drawing(@PathVariable Long id) {
        var drawing = service.drawing(AuthUtils.getCurrentUserIdOrThrow(), id);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(drawing.contentType()))
                .header("X-Content-Type-Options", "nosniff")
                .body(drawing.bytes());
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    org.springframework.web.servlet.ModelAndView missing(org.springframework.web.server.ResponseStatusException ex) {
        var view = new org.springframework.web.servlet.ModelAndView("error/404");
        view.setStatus(ex.getStatusCode());
        view.addObject("message", ex.getReason());
        return view;
    }
}
