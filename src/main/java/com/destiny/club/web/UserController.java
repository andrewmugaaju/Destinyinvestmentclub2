package com.destiny.club.web;

import com.destiny.club.domain.user.RoleName;
import com.destiny.club.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userService.findAll());
        return "users/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("roleOptions", RoleName.values());
        return "users/form";
    }

    @PostMapping
    public String create(@RequestParam String username,
                          @RequestParam String password,
                          @RequestParam String fullName,
                          @RequestParam(required = false) String email,
                          @RequestParam(required = false) java.util.List<String> roles,
                          RedirectAttributes redirectAttributes) {
        Set<RoleName> roleNames = toRoleNames(roles);
        userService.create(username, password, fullName, email, roleNames);
        redirectAttributes.addFlashAttribute("successMessage", "User " + username + " created");
        return "redirect:/users";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("user", userService.getById(id));
        model.addAttribute("roleOptions", RoleName.values());
        return "users/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                          @RequestParam(required = false) java.util.List<String> roles,
                          @RequestParam(required = false) Boolean enabled,
                          RedirectAttributes redirectAttributes) {
        Set<RoleName> roleNames = toRoleNames(roles);
        userService.updateRolesAndStatus(id, roleNames, enabled != null && enabled);
        redirectAttributes.addFlashAttribute("successMessage", "User updated");
        return "redirect:/users";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id, @RequestParam String newPassword, RedirectAttributes redirectAttributes) {
        userService.resetPassword(id, newPassword);
        redirectAttributes.addFlashAttribute("successMessage", "Password reset");
        return "redirect:/users/" + id + "/edit";
    }

    private Set<RoleName> toRoleNames(java.util.List<String> roles) {
        if (roles == null) {
            return Set.of();
        }
        return roles.stream().map(RoleName::valueOf).collect(Collectors.toSet());
    }
}
