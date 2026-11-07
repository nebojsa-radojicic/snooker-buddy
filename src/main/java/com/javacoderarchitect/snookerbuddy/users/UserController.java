package com.javacoderarchitect.snookerbuddy.users;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.HashSet;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
class UserController {
    private final UserService userService;

    UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    String registrationForm() {
        return "login";
    }

    @GetMapping("/registration")
    String registrationForm(Model model) {
        model.addAttribute("user", new UserRegistrationForm("", "", "", false, false));
        return "registration";
    }

    @PostMapping("/registration")
    String registerUser(
            @ModelAttribute("user") @Valid UserRegistrationForm userRegistrationForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "registration";
        }
        try {
            var roles = new HashSet<Role>();
            if (userRegistrationForm.coach()) roles.add(Role.ROLE_COACH);
            if (userRegistrationForm.player()) roles.add(Role.ROLE_PLAYER);
            var cmd = new CreateUserCmd(
                    userRegistrationForm.name(), userRegistrationForm.email(), userRegistrationForm.password(), roles);
            userService.createUser(cmd);
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please login.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Registration failed: " + e.getMessage());
            return "redirect:/registration";
        }
    }

    public record UserRegistrationForm(
            @NotBlank(message = "Name is required") String name,

            @NotBlank(message = "Email is required") @Email(message = "Invalid email address")
            String email,

            @NotBlank(message = "Password is required") String password,
            Boolean coach,
            Boolean player) {
        public UserRegistrationForm {
            coach = Boolean.TRUE.equals(coach);
            player = Boolean.TRUE.equals(player);
        }

        @AssertTrue(message = "Choose coach, player, or both")
        public boolean isRoleSelected() {
            return coach || player;
        }
    }
}
