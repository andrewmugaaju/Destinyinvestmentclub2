package com.destiny.club.web;

import com.destiny.club.domain.client.Group;
import com.destiny.club.service.GroupService;
import com.destiny.club.service.LoanService;
import com.destiny.club.service.SavingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/groups")
public class GroupController {

    private final GroupService groupService;
    private final SavingsService savingsService;
    private final LoanService loanService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("groups", groupService.findAll());
        return "groups/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("group", new Group());
        return "groups/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("group", groupService.getById(id));
        return "groups/form";
    }

    @PostMapping
    public String save(@ModelAttribute("group") Group group, RedirectAttributes redirectAttributes) {
        groupService.save(group);
        redirectAttributes.addFlashAttribute("successMessage", "Group saved successfully");
        return "redirect:/groups";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        Group group = groupService.getById(id);
        model.addAttribute("group", group);
        model.addAttribute("members", groupService.members(id));
        model.addAttribute("savingsAccounts", savingsService.findByGroup(id));
        model.addAttribute("loanAccounts", loanService.findByGroup(id));
        return "groups/view";
    }
}
