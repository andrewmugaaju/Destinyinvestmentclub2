package com.destiny.club.web;

import com.destiny.club.domain.accounting.GLCodes;
import com.destiny.club.domain.loan.LoanStatus;
import com.destiny.club.repository.ClientRepository;
import com.destiny.club.repository.GroupRepository;
import com.destiny.club.service.AccountingService;
import com.destiny.club.service.LoanService;
import com.destiny.club.service.SavingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final ClientRepository clientRepository;
    private final GroupRepository groupRepository;
    private final SavingsService savingsService;
    private final LoanService loanService;
    private final AccountingService accountingService;

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalClients", clientRepository.count());
        model.addAttribute("totalGroups", groupRepository.count());

        BigDecimal totalSavings = savingsService.findAll().stream()
                .map(a -> a.getBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("totalSavings", totalSavings);

        BigDecimal totalOutstandingLoans = loanService.findAll().stream()
                .filter(l -> l.getStatus() == LoanStatus.ACTIVE)
                .map(l -> l.getTotalOutstanding())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("totalOutstandingLoans", totalOutstandingLoans);

        long pendingLoans = loanService.findAll().stream().filter(l -> l.getStatus() == LoanStatus.PENDING).count();
        model.addAttribute("pendingLoans", pendingLoans);

        BigDecimal cashBalance;
        try {
            var account = accountingService.getAccountByCode(GLCodes.CASH_AND_BANK);
            var ledger = accountingService.generateGeneralLedger(account.getId(), null, LocalDate.now());
            cashBalance = ledger.getClosingBalance();
        } catch (Exception ex) {
            cashBalance = BigDecimal.ZERO;
        }
        model.addAttribute("cashBalance", cashBalance);

        model.addAttribute("recentEntries", accountingService.recentEntries().stream().limit(10).toList());

        return "dashboard";
    }
}
