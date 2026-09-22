package com.destiny.club.web;

import com.destiny.club.domain.client.Client;
import com.destiny.club.domain.client.Group;
import com.destiny.club.domain.savings.SavingsAccount;
import com.destiny.club.domain.savings.SavingsAccountStatus;
import com.destiny.club.security.CustomUserDetails;
import com.destiny.club.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/savings-accounts")
public class SavingsAccountController {

    private final SavingsService savingsService;
    private final SavingsProductService savingsProductService;
    private final ClientService clientService;
    private final GroupService groupService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("accounts", savingsService.findAll());
        return "savings-accounts/list";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long clientId,
                           @RequestParam(required = false) Long groupId,
                           Model model) {
        model.addAttribute("clients", clientService.findAll());
        model.addAttribute("groups", groupService.findAll());
        model.addAttribute("products", savingsProductService.findActive());
        model.addAttribute("selectedClientId", clientId);
        model.addAttribute("selectedGroupId", groupId);
        return "savings-accounts/form";
    }

    @PostMapping
    public String open(@RequestParam(required = false) Long clientId,
                        @RequestParam(required = false) Long groupId,
                        @RequestParam Long productId,
                        @RequestParam(required = false) LocalDate openedDate,
                        RedirectAttributes redirectAttributes) {
        Client client = clientId != null ? clientService.getById(clientId) : null;
        Group group = groupId != null ? groupService.getById(groupId) : null;
        var product = savingsProductService.getById(productId);
        SavingsAccount account = savingsService.openAccount(client, group, product, openedDate);
        redirectAttributes.addFlashAttribute("successMessage", "Savings account " + account.getAccountNumber() + " opened");
        return "redirect:/savings-accounts/" + account.getId();
    }

    @GetMapping("/deposit")
    public String depositForm(@RequestParam(required = false) Long clientId,
                               @RequestParam(required = false) Long groupId,
                               Model model) {
        model.addAttribute("clients", clientService.findAll());
        model.addAttribute("groups", groupService.findAll());
        model.addAttribute("selectedClientId", clientId);
        model.addAttribute("selectedGroupId", groupId);

        List<SavingsAccount> accounts;
        if (clientId != null) {
            accounts = savingsService.findByClient(clientId);
        } else if (groupId != null) {
            accounts = savingsService.findByGroup(groupId);
        } else {
            accounts = Collections.emptyList();
        }
        model.addAttribute("savingsAccounts", accounts.stream()
                .filter(a -> a.getStatus() == SavingsAccountStatus.ACTIVE)
                .toList());
        return "savings-accounts/deposit-form";
    }

    @PostMapping("/deposit")
    public String deposit(@RequestParam Long savingsAccountId,
                           @RequestParam BigDecimal amount,
                           @RequestParam(required = false) LocalDate transactionDate,
                           @RequestParam(required = false) String narration,
                           @AuthenticationPrincipal CustomUserDetails principal,
                           RedirectAttributes redirectAttributes) {
        SavingsAccount account = savingsService.getById(savingsAccountId);
        savingsService.depositWithPosting(account, amount, transactionDate, narration, principal.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "Deposit recorded to " + account.getAccountNumber());
        return "redirect:/savings-accounts/" + account.getId();
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        SavingsAccount account = savingsService.getById(id);
        model.addAttribute("account", account);
        model.addAttribute("transactions", savingsService.transactionsFor(id));
        return "savings-accounts/view";
    }

    @PostMapping("/{id}/withdraw")
    public String withdraw(@PathVariable Long id,
                            @RequestParam BigDecimal amount,
                            @RequestParam(required = false) LocalDate transactionDate,
                            @RequestParam(required = false) String narration,
                            @AuthenticationPrincipal CustomUserDetails principal,
                            RedirectAttributes redirectAttributes) {
        SavingsAccount account = savingsService.getById(id);
        savingsService.withdraw(account, amount, transactionDate, narration, principal.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "Withdrawal recorded");
        return "redirect:/savings-accounts/" + id;
    }
}
