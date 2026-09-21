package com.destiny.club.web;

import com.destiny.club.domain.client.Client;
import com.destiny.club.service.ClientService;
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
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;
    private final GroupService groupService;
    private final SavingsService savingsService;
    private final LoanService loanService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("clients", clientService.findAll());
        return "clients/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("client", new Client());
        model.addAttribute("groups", groupService.findAll());
        return "clients/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("client", clientService.getById(id));
        model.addAttribute("groups", groupService.findAll());
        return "clients/form";
    }

    @PostMapping
    public String save(@ModelAttribute Client client,
                        @RequestParam(required = false) Long groupId,
                        RedirectAttributes redirectAttributes) {
        clientService.save(client, groupId);
        redirectAttributes.addFlashAttribute("successMessage", "Client saved successfully");
        return "redirect:/clients";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        Client client = clientService.getById(id);
        model.addAttribute("client", client);
        model.addAttribute("savingsAccounts", savingsService.findByClient(id));
        model.addAttribute("loanAccounts", loanService.findByClient(id));
        return "clients/view";
    }
}
