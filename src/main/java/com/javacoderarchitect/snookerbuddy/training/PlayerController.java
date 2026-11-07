package com.javacoderarchitect.snookerbuddy.training;

import com.javacoderarchitect.snookerbuddy.users.AuthUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/players")
class PlayerController {
    private final PlayerService service;

    PlayerController(PlayerService service) {
        this.service = service;
    }

    @GetMapping
    String players(Model model) {
        model.addAttribute("player", new AddPlayerForm(""));
        return page(model);
    }

    @PostMapping
    String add(
            @Valid @ModelAttribute("player") AddPlayerForm form,
            BindingResult errors,
            Model model,
            RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                service.add(AuthUtils.getCurrentUserIdOrThrow(), form.email());
                redirect.addFlashAttribute("successMessage", "Player added. You can now assign training.");
                return "redirect:/training/new";
            } catch (IllegalArgumentException ex) {
                errors.rejectValue("email", "player.invalid", ex.getMessage());
            } catch (DataIntegrityViolationException ex) {
                errors.rejectValue("email", "player.linked", "This player already has a coach.");
            }
        }
        return page(model);
    }

    private String page(Model model) {
        model.addAttribute("players", service.players(AuthUtils.getCurrentUserIdOrThrow()));
        return "training/players";
    }

    public record AddPlayerForm(
            @NotBlank(message = "Enter the player's email") @Email(message = "Enter a valid email address")
            String email) {
        public AddPlayerForm {
            email = email == null ? null : email.strip();
        }
    }
}
