package com.destiny.club.web;

import com.destiny.club.domain.accounting.EntryType;
import com.destiny.club.domain.accounting.GLAccount;
import com.destiny.club.domain.accounting.JournalEntry;
import com.destiny.club.domain.accounting.JournalEntryLine;
import com.destiny.club.exception.NotFoundException;
import com.destiny.club.repository.GLAccountRepository;
import com.destiny.club.repository.JournalEntryRepository;
import com.destiny.club.security.CustomUserDetails;
import com.destiny.club.service.AccountingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/journal")
public class JournalController {

    private final AccountingService accountingService;
    private final GLAccountRepository glAccountRepository;
    private final JournalEntryRepository journalEntryRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("entries", accountingService.recentEntries());
        return "journal/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("accounts", glAccountRepository.findAllByOrderByCode());
        return "journal/form";
    }

    @PostMapping
    public String save(@RequestParam LocalDate transactionDate,
                        @RequestParam String description,
                        @RequestParam List<Long> lineAccount,
                        @RequestParam List<String> lineType,
                        @RequestParam List<BigDecimal> lineAmount,
                        @AuthenticationPrincipal CustomUserDetails principal,
                        RedirectAttributes redirectAttributes) {
        List<JournalEntryLine> lines = new ArrayList<>();
        for (int i = 0; i < lineAccount.size(); i++) {
            GLAccount account = glAccountRepository.findById(lineAccount.get(i))
                    .orElseThrow(() -> new NotFoundException("GL account not found"));
            EntryType type = EntryType.valueOf(lineType.get(i));
            lines.add(type == EntryType.DEBIT
                    ? JournalEntryLine.debit(account, lineAmount.get(i), null)
                    : JournalEntryLine.credit(account, lineAmount.get(i), null));
        }

        JournalEntry entry = accountingService.post(transactionDate, "MANUAL", null, description, principal.getUsername(), lines);
        redirectAttributes.addFlashAttribute("successMessage", "Journal entry " + entry.getReference() + " posted");
        return "redirect:/journal/" + entry.getId();
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        JournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Journal entry not found: " + id));
        model.addAttribute("entry", entry);
        return "journal/view";
    }
}
