package com.destiny.club.web;

import com.destiny.club.domain.client.Client;
import com.destiny.club.domain.deposit.AllocationType;
import com.destiny.club.domain.deposit.DepositAllocation;
import com.destiny.club.domain.deposit.DepositTransaction;
import com.destiny.club.domain.savings.SavingsAccount;
import com.destiny.club.domain.savings.SavingsAccountStatus;
import com.destiny.club.dto.DepositAllocationForm;
import com.destiny.club.dto.DepositAllocationView;
import com.destiny.club.dto.DepositForm;
import com.destiny.club.exception.BusinessException;
import com.destiny.club.security.CustomUserDetails;
import com.destiny.club.service.AccountingService;
import com.destiny.club.service.ClientService;
import com.destiny.club.service.DepositService;
import com.destiny.club.service.GroupService;
import com.destiny.club.service.LoanService;
import com.destiny.club.service.SavingsProductService;
import com.destiny.club.service.SavingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/deposits")
public class DepositController {

    private final DepositService depositService;
    private final ClientService clientService;
    private final GroupService groupService;
    private final SavingsService savingsService;
    private final SavingsProductService savingsProductService;
    private final LoanService loanService;
    private final AccountingService accountingService;

    @GetMapping
    public String list(Model model) {
        List<DepositTransaction> deposits = depositService.findAll();
        Map<Long, String> savingsProductByDepositId = new HashMap<>();
        for (DepositTransaction d : deposits) {
            d.getAllocations().stream()
                    .filter(a -> a.getAllocationType() == AllocationType.SAVINGS_DEPOSIT)
                    .findFirst()
                    .ifPresent(a -> savingsProductByDepositId.put(d.getId(),
                            savingsService.getById(a.getTargetAccountId()).getSavingsProduct().getName()));
        }
        model.addAttribute("deposits", deposits);
        model.addAttribute("savingsProductByDepositId", savingsProductByDepositId);
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
        // Savings products drive the split, same as the Savings Deposit screen: pick a product,
        // and if there's no active account under it yet, one is opened automatically on submit -
        // there is no separate "open an account" step.
        model.addAttribute("savingsProducts", savingsProductService.findActive());

        String selectedMemberLabel = null;
        if (clientId != null) {
            List<SavingsAccount> existing = filterActive(savingsService.findByClient(clientId));
            Map<Long, SavingsAccount> existingByProductId = existing.stream()
                    .collect(Collectors.toMap(a -> a.getSavingsProduct().getId(), a -> a, (a, b) -> a));
            model.addAttribute("existingByProductId", existingByProductId);
            model.addAttribute("loanAccounts", loanService.findActiveByClient(clientId));
            var client = clientService.getById(clientId);
            selectedMemberLabel = client.getFullName() + " (" + client.getClientNumber() + ")";
        } else if (groupId != null) {
            // Groups no longer have their own savings accounts - only loan repayments and shares apply.
            model.addAttribute("loanAccounts", loanService.findActiveByGroup(groupId));
            selectedMemberLabel = groupService.getById(groupId).getGroupName() + " (Group)";
        } else {
            model.addAttribute("loanAccounts", Collections.emptyList());
        }
        model.addAttribute("selectedMemberLabel", selectedMemberLabel);
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
                        @RequestParam(required = false) String receiptNumber,
                        @RequestParam(required = false) String narration,
                        @RequestParam(required = false) Long savingsProductId,
                        @RequestParam(required = false) BigDecimal savingsAmount,
                        @RequestParam(required = false) BigDecimal sharesAmount,
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
        form.setReceiptNumber(receiptNumber);
        form.setNarration(narration);

        List<DepositAllocationForm> allocations = new ArrayList<>();
        if (savingsProductId != null && savingsAmount != null && savingsAmount.compareTo(BigDecimal.ZERO) > 0) {
            // No separate "open an account" step - if the client doesn't already have an active
            // account under this product, one is opened automatically as part of the deposit.
            Client client = clientService.getById(clientId);
            var product = savingsProductService.getById(savingsProductId);
            SavingsAccount account = savingsService.findOrOpenAccount(client, product);
            DepositAllocationForm a = new DepositAllocationForm();
            a.setAllocationType(AllocationType.SAVINGS_DEPOSIT);
            a.setTargetAccountId(account.getId());
            a.setAmount(savingsAmount);
            allocations.add(a);
        }
        if (sharesAmount != null && sharesAmount.compareTo(BigDecimal.ZERO) > 0) {
            DepositAllocationForm a = new DepositAllocationForm();
            a.setAllocationType(AllocationType.SHARE_PURCHASE);
            a.setAmount(sharesAmount);
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
    public String view(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal, Model model) {
        DepositTransaction deposit = depositService.getById(id);
        model.addAttribute("deposit", deposit);
        model.addAttribute("allocationViews", deposit.getAllocations().stream().map(this::toView).toList());
        model.addAttribute("canManageDeposit", canManage(deposit, principal));
        return "deposits/view";
    }

    /**
     * ADMIN/MANAGER can edit or void any deposit. A Loan Officer - who has no other screen at
     * all - can only edit or void deposits they themselves recorded; everyone else (tellers,
     * accountants) has no edit/void access here, unchanged from before.
     */
    private boolean canManage(DepositTransaction deposit, CustomUserDetails principal) {
        if (hasAnyRole(principal, "ROLE_ADMIN", "ROLE_MANAGER")) {
            return true;
        }
        return hasAnyRole(principal, "ROLE_LOAN_OFFICER")
                && principal.getUsername().equals(deposit.getCreatedBy());
    }

    private boolean hasAnyRole(CustomUserDetails principal, String... authorities) {
        List<String> wanted = List.of(authorities);
        for (GrantedAuthority authority : principal.getAuthorities()) {
            if (wanted.contains(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private void requireCanManage(Long depositId, CustomUserDetails principal) {
        DepositTransaction deposit = depositService.getById(depositId);
        if (!canManage(deposit, principal)) {
            throw new BusinessException("You can only edit or void deposits you created yourself");
        }
    }

    private DepositAllocationView toView(DepositAllocation allocation) {
        if (allocation.getAllocationType() == AllocationType.SAVINGS_DEPOSIT) {
            var account = savingsService.getById(allocation.getTargetAccountId());
            return new DepositAllocationView(allocation.getAllocationType(), account.getAccountNumber(),
                    account.getSavingsProduct().getName(), allocation.getAmount());
        }
        if (allocation.getAllocationType() == AllocationType.SHARE_PURCHASE) {
            return new DepositAllocationView(allocation.getAllocationType(), "-", "Member Share Capital", allocation.getAmount());
        }
        var loan = loanService.getById(allocation.getTargetAccountId());
        return new DepositAllocationView(allocation.getAllocationType(), loan.getLoanAccountNumber(),
                loan.getLoanProduct().getName(), allocation.getAmount());
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','LOAN_OFFICER')")
    public String edit(@PathVariable Long id,
                        @RequestParam(required = false) String narration,
                        @AuthenticationPrincipal CustomUserDetails principal,
                        RedirectAttributes redirectAttributes) {
        requireCanManage(id, principal);
        depositService.updateNarration(id, narration, principal.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "Deposit updated");
        return "redirect:/deposits/" + id;
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','LOAN_OFFICER')")
    public String voidDeposit(@PathVariable Long id,
                               @RequestParam(required = false) String reason,
                               @AuthenticationPrincipal CustomUserDetails principal,
                               RedirectAttributes redirectAttributes) {
        requireCanManage(id, principal);
        depositService.voidDeposit(id, principal.getUsername(), reason);
        redirectAttributes.addFlashAttribute("successMessage", "Deposit voided");
        return "redirect:/deposits/" + id;
    }
}
