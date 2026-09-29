package com.destiny.club.web;

import com.destiny.club.domain.client.Client;
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
    private final AccountingService accountingService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("accounts", savingsService.findAll());
        return "savings-accounts/list";
    }

    /**
     * The Savings Deposit screen: pick a member, then a savings product. There is no
     * separate "open an account" step - if they don't already have an active account under that
     * product, one is opened automatically as part of recording the deposit.
     */
    @GetMapping("/deposit")
    public String depositForm(@RequestParam(required = false) Long clientId,
                               Model model) {
        model.addAttribute("clients", clientService.findAll());
        model.addAttribute("selectedClientId", clientId);
        model.addAttribute("products", savingsProductService.findActive());
        model.addAttribute("cashAccounts", accountingService.findCashAccounts());

        if (clientId != null) {
            List<SavingsAccount> existing = savingsService.findByClient(clientId);
            Map<Long, SavingsAccount> byProductId = existing.stream()
                    .filter(a -> a.getStatus() == SavingsAccountStatus.ACTIVE)
                    .collect(Collectors.toMap(a -> a.getSavingsProduct().getId(), a -> a, (a, b) -> a));
            model.addAttribute("existingByProductId", byProductId);
        }
        return "savings-accounts/deposit-form";
    }

    @PostMapping("/deposit")
    public String deposit(@RequestParam Long clientId,
                           @RequestParam Long productId,
                           @RequestParam Long cashAccountId,
                           @RequestParam BigDecimal amount,
                           @RequestParam(required = false) LocalDate transactionDate,
                           @RequestParam(required = false) String narration,
                           @AuthenticationPrincipal CustomUserDetails principal,
                           RedirectAttributes redirectAttributes) {
        Client client = clientService.getById(clientId);
        SavingsProduct product = savingsProductService.getById(productId);
        var cashAccount = accountingService.getCashAccountById(cashAccountId);

        SavingsAccount account = savingsService.findOrOpenAccount(client, product);
        savingsService.depositWithPosting(account, amount, transactionDate, narration, principal.getUsername(), cashAccount);
        redirectAttributes.addFlashAttribute("successMessage", "Deposit recorded to " + account.getAccountNumber());
        return "redirect:/savings-accounts/" + account.getId();
    }

    /**
     * The Savings Withdrawal screen: pick a member, then one of their existing active
     * savings accounts to withdraw from (unlike deposits, there is nothing to auto-open here -
     * you can only withdraw from an account that already exists).
     */
    @GetMapping("/withdraw")
    public String withdrawForm(@RequestParam(required = false) Long clientId,
                                Model model) {
        model.addAttribute("clients", clientService.findAll());
        model.addAttribute("selectedClientId", clientId);
        model.addAttribute("cashAccounts", accountingService.findCashAccounts());

        List<SavingsAccount> accounts = clientId != null ? savingsService.findByClient(clientId) : List.of();
        model.addAttribute("savingsAccounts", accounts.stream()
                .filter(a -> a.getStatus() == SavingsAccountStatus.ACTIVE)
                .toList());
        return "savings-accounts/withdraw-form";
    }

    @PostMapping("/withdraw")
    public String withdrawSubmit(@RequestParam Long savingsAccountId,
                                  @RequestParam BigDecimal amount,
                                  @RequestParam Long cashAccountId,
                                  @RequestParam(required = false) LocalDate transactionDate,
                                  @RequestParam(required = false) String narration,
                                  @AuthenticationPrincipal CustomUserDetails principal,
                                  RedirectAttributes redirectAttributes) {
        SavingsAccount account = savingsService.getById(savingsAccountId);
        var cashAccount = accountingService.getCashAccountById(cashAccountId);
        savingsService.withdraw(account, amount, transactionDate, narration, principal.getUsername(), cashAccount);
        redirectAttributes.addFlashAttribute("successMessage", "Withdrawal recorded from " + account.getAccountNumber());
        return "redirect:/savings-accounts/" + account.getId();
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        SavingsAccount account = savingsService.getById(id);
        model.addAttribute("account", account);
        model.addAttribute("transactions", savingsService.transactionsFor(id));
        model.addAttribute("cashAccounts", accountingService.findCashAccounts());
        return "savings-accounts/view";
    }

    @PostMapping("/{id}/withdraw")
    public String withdraw(@PathVariable Long id,
                            @RequestParam BigDecimal amount,
                            @RequestParam Long cashAccountId,
                            @RequestParam(required = false) LocalDate transactionDate,
                            @RequestParam(required = false) String narration,
                            @AuthenticationPrincipal CustomUserDetails principal,
                            RedirectAttributes redirectAttributes) {
        SavingsAccount account = savingsService.getById(id);
        var cashAccount = accountingService.getCashAccountById(cashAccountId);
        savingsService.withdraw(account, amount, transactionDate, narration, principal.getUsername(), cashAccount);
        redirectAttributes.addFlashAttribute("successMessage", "Withdrawal recorded");
        return "redirect:/savings-accounts/" + id;
    }
}
