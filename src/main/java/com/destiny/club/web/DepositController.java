package com.destiny.club.web;

import com.destiny.club.domain.deposit.AllocationType;
import com.destiny.club.domain.deposit.DepositAllocation;
import com.destiny.club.domain.deposit.DepositTransaction;
import com.destiny.club.domain.savings.SavingsAccountStatus;
import com.destiny.club.dto.DepositAllocationForm;
import com.destiny.club.dto.DepositAllocationView;
import com.destiny.club.dto.DepositForm;
import com.destiny.club.security.CustomUserDetails;
import com.destiny.club.service.AccountingService;
import com.destiny.club.service.ClientService;
import com.destiny.club.service.DepositService;
import com.destiny.club.service.GroupService;
import com.destiny.club.service.LoanService;
import com.destiny.club.service.SavingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/deposits")
public class DepositController {

    private final DepositService depositService;
    private final ClientService clientService;
    private final GroupService groupService;
    private final SavingsService savingsService;
    private final LoanService loanService;
    private final AccountingService accountingService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("deposits", depositService.findAll());
        return "deposits/list";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long clientId,
                           @RequestParam(required = false) Long groupId,
                           Model model) {
        model.addAttribute("clients", clientService.findAll());
        model.addAttribute("groups", groupService.findAll());
        model.addAttribute("selectedClientId", clientId);
        model.addAttribute("selectedGroupId", groupId);
        model.addAttribute("cashAccounts", accountingService.findCashAccounts());

        if (clientId != null) {
            model.addAttribute("savingsAccounts", filterActive(savingsService.findByClient(clientId)));
            model.addAttribute("loanAccounts", loanService.findActiveByClient(clientId));
        } else if (groupId != null) {
            model.addAttribute("savingsAccounts", filterActive(savingsService.findByGroup(groupId)));
            model.addAttribute("loanAccounts", loanService.findActiveByGroup(groupId));
        } else {
            model.addAttribute("savingsAccounts", Collections.emptyList());
            model.addAttribute("loanAccounts", Collections.emptyList());
        }
        return "deposits/form";
    }

    private List<com.destiny.club.domain.savings.SavingsAccount> filterActive(List<com.destiny.club.domain.savings.SavingsAccount> accounts) {
        return accounts.stream().filter(a -> a.getStatus() == SavingsAccountStatus.ACTIVE).toList();
    }

    @PostMapping
    public String save(@RequestParam(required = false) Long clientId,
                        @RequestParam(required = false) Long groupId,
                        @RequestParam Long cashAccountId,
                        @RequestParam LocalDate transactionDate,
                        @RequestParam BigDecimal totalAmount,
                        @RequestParam(required = false) String narration,
                        @RequestParam(required = false) Long savingsAccountId,
                        @RequestParam(required = false) BigDecimal savingsAmount,
                        @RequestParam(required = false) Long loanAccountId,
                        @RequestParam(required = false) BigDecimal loanAmount,
                        @AuthenticationPrincipal CustomUserDetails principal,
                        RedirectAttributes redirectAttributes) {
        DepositForm form = new DepositForm();
        form.setClientId(clientId);
        form.setGroupId(groupId);
        form.setCashAccountId(cashAccountId);
        form.setTransactionDate(transactionDate);
        form.setTotalAmount(totalAmount);
        form.setNarration(narration);

        List<DepositAllocationForm> allocations = new ArrayList<>();
        if (savingsAccountId != null && savingsAmount != null && savingsAmount.compareTo(BigDecimal.ZERO) > 0) {
            DepositAllocationForm a = new DepositAllocationForm();
            a.setAllocationType(AllocationType.SAVINGS_DEPOSIT);
            a.setTargetAccountId(savingsAccountId);
            a.setAmount(savingsAmount);
            allocations.add(a);
        }
        if (loanAccountId != null && loanAmount != null && loanAmount.compareTo(BigDecimal.ZERO) > 0) {
            DepositAllocationForm a = new DepositAllocationForm();
            a.setAllocationType(AllocationType.LOAN_REPAYMENT);
            a.setTargetAccountId(loanAccountId);
            a.setAmount(loanAmount);
            allocations.add(a);
        }
        form.setAllocations(allocations);

        DepositTransaction deposit = depositService.record(form, principal.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "Deposit " + deposit.getReference() + " recorded successfully");
        return "redirect:/deposits/" + deposit.getId();
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        DepositTransaction deposit = depositService.getById(id);
        model.addAttribute("deposit", deposit);
        model.addAttribute("allocationViews", deposit.getAllocations().stream().map(this::toView).toList());
        return "deposits/view";
    }

    private DepositAllocationView toView(DepositAllocation allocation) {
        if (allocation.getAllocationType() == AllocationType.SAVINGS_DEPOSIT) {
            var account = savingsService.getById(allocation.getTargetAccountId());
            return new DepositAllocationView(allocation.getAllocationType(), account.getAccountNumber(),
                    account.getSavingsProduct().getName(), allocation.getAmount());
        }
        var loan = loanService.getById(allocation.getTargetAccountId());
        return new DepositAllocationView(allocation.getAllocationType(), loan.getLoanAccountNumber(),
                loan.getLoanProduct().getName(), allocation.getAmount());
    }
}
