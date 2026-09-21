package com.destiny.club.web;

import com.destiny.club.domain.client.Client;
import com.destiny.club.domain.client.Group;
import com.destiny.club.domain.loan.LoanAccount;
import com.destiny.club.security.CustomUserDetails;
import com.destiny.club.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
@RequestMapping("/loan-accounts")
public class LoanAccountController {

    private final LoanService loanService;
    private final LoanProductService loanProductService;
    private final ClientService clientService;
    private final GroupService groupService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("loans", loanService.findAll());
        return "loan-accounts/list";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long clientId,
                           @RequestParam(required = false) Long groupId,
                           Model model) {
        model.addAttribute("clients", clientService.findAll());
        model.addAttribute("groups", groupService.findAll());
        model.addAttribute("products", loanProductService.findActive());
        model.addAttribute("selectedClientId", clientId);
        model.addAttribute("selectedGroupId", groupId);
        return "loan-accounts/form";
    }

    @PostMapping
    public String apply(@RequestParam(required = false) Long clientId,
                         @RequestParam(required = false) Long groupId,
                         @RequestParam Long productId,
                         @RequestParam BigDecimal principalAmount,
                         @RequestParam(required = false) Integer termMonths,
                         @RequestParam(required = false) BigDecimal applicationFeeAmount,
                         RedirectAttributes redirectAttributes) {
        Client client = clientId != null ? clientService.getById(clientId) : null;
        Group group = groupId != null ? groupService.getById(groupId) : null;
        var product = loanProductService.getById(productId);
        LoanAccount loan = loanService.apply(client, group, product, principalAmount, termMonths, applicationFeeAmount);
        redirectAttributes.addFlashAttribute("successMessage", "Loan application " + loan.getLoanAccountNumber() + " submitted");
        return "redirect:/loan-accounts/" + loan.getId();
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        LoanAccount loan = loanService.getById(id);
        model.addAttribute("loan", loan);
        model.addAttribute("transactions", loanService.transactionsFor(id));
        return "loan-accounts/view";
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String approve(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal, RedirectAttributes redirectAttributes) {
        loanService.approve(id, principal.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "Loan approved");
        return "redirect:/loan-accounts/" + id;
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String reject(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal, RedirectAttributes redirectAttributes) {
        loanService.reject(id, principal.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "Loan rejected");
        return "redirect:/loan-accounts/" + id;
    }

    @PostMapping("/{id}/disburse")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String disburse(@PathVariable Long id,
                            @RequestParam(required = false) LocalDate disbursementDate,
                            @AuthenticationPrincipal CustomUserDetails principal,
                            RedirectAttributes redirectAttributes) {
        loanService.disburse(id, disbursementDate, principal.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "Loan disbursed and repayment schedule generated");
        return "redirect:/loan-accounts/" + id;
    }

    @PostMapping("/{id}/repay")
    public String repay(@PathVariable Long id,
                         @RequestParam BigDecimal amount,
                         @RequestParam(required = false) LocalDate transactionDate,
                         @RequestParam(required = false) String narration,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes) {
        LoanAccount loan = loanService.getById(id);
        loanService.repayWithPosting(loan, amount, transactionDate, narration, principal.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "Repayment recorded");
        return "redirect:/loan-accounts/" + id;
    }
}
