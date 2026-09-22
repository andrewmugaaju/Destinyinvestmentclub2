package com.destiny.club.web;

import com.destiny.club.domain.client.Client;
import com.destiny.club.domain.client.Group;
import com.destiny.club.domain.savings.SavingsAccount;
import com.destiny.club.domain.savings.SavingsAccountStatus;
import com.destiny.club.domain.savings.SavingsProduct;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    /**
     * The Savings Deposit screen: pick a member/group, then a savings product. There is no
     * separate "open an account" step - if they don't already have an active account under that
     * product, one is opened automatically as part of recording the deposit.
     */
    @GetMapping("/deposit")
    public String depositForm(@RequestParam(required = false) Long clientId,
                               @RequestParam(required = false) Long groupId,
                               Model model) {
        model.addAttribute("clients", clientService.findAll());
        model.addAttribute("groups", groupService.findAll());
        model.addAttribute("selectedClientId", clientId);
        model.addAttribute("selectedGroupId", groupId);
        model.addAttribute("products", savingsProductService.findActive());

        if (clientId != null || groupId != null) {
            List<SavingsAccount> existing = clientId != null
                    ? savingsService.findByClient(clientId)
                    : savingsService.findByGroup(groupId);
            Map<Long, SavingsAccount> byProductId = existing.stream()
                    .filter(a -> a.getStatus() == SavingsAccountStatus.ACTIVE)
                    .collect(Collectors.toMap(a -> a.getSavingsProduct().getId(), a -> a, (a, b) -> a));
            model.addAttribute("existingByProductId", byProductId);
        }
        return "savings-accounts/deposit-form";
    }

    @PostMapping("/deposit")
    public String deposit(@RequestParam(required = false) Long clientId,
                           @RequestParam(required = false) Long groupId,
                           @RequestParam Long productId,
                           @RequestParam BigDecimal amount,
                           @RequestParam(required = false) LocalDate transactionDate,
                           @RequestParam(required = false) String narration,
                           @AuthenticationPrincipal CustomUserDetails principal,
                           RedirectAttributes redirectAttributes) {
        Client client = clientId != null ? clientService.getById(clientId) : null;
        Group group = groupId != null ? groupService.getById(groupId) : null;
        SavingsProduct product = savingsProductService.getById(productId);

        SavingsAccount account = savingsService.findOrOpenAccount(client, group, product);
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
