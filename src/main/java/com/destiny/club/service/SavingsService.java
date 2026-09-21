package com.destiny.club.service;

import com.destiny.club.domain.accounting.GLAccount;
import com.destiny.club.domain.accounting.GLCodes;
import com.destiny.club.domain.accounting.JournalEntryLine;
import com.destiny.club.domain.client.Client;
import com.destiny.club.domain.client.Group;
import com.destiny.club.domain.savings.*;
import com.destiny.club.exception.BusinessException;
import com.destiny.club.exception.NotFoundException;
import com.destiny.club.repository.SavingsAccountRepository;
import com.destiny.club.repository.SavingsTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SavingsService {

    private final SavingsAccountRepository savingsAccountRepository;
    private final SavingsTransactionRepository savingsTransactionRepository;
    private final AccountingService accountingService;

    public List<SavingsAccount> findAll() {
        return savingsAccountRepository.findAll();
    }

    public SavingsAccount getById(Long id) {
        return savingsAccountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Savings account not found: " + id));
    }

    public List<SavingsAccount> findByClient(Long clientId) {
        return savingsAccountRepository.findByClientId(clientId);
    }

    public List<SavingsAccount> findByGroup(Long groupId) {
        return savingsAccountRepository.findByGroupId(groupId);
    }

    public List<SavingsTransaction> transactionsFor(Long savingsAccountId) {
        return savingsTransactionRepository.findBySavingsAccountIdOrderByTransactionDateDescIdDesc(savingsAccountId);
    }

    @Transactional
    public SavingsAccount openAccount(Client client, Group group, SavingsProduct product, LocalDate openedDate) {
        if (client == null && group == null) {
            throw new BusinessException("A savings account must belong to a client or a group");
        }
        SavingsAccount account = new SavingsAccount();
        account.setClient(client);
        account.setGroup(group);
        account.setSavingsProduct(product);
        account.setOpenedDate(openedDate != null ? openedDate : LocalDate.now());
        account.setAccountNumber(generateAccountNumber());
        account.setBalance(BigDecimal.ZERO);
        return savingsAccountRepository.save(account);
    }

    private String generateAccountNumber() {
        long seq = savingsAccountRepository.count() + 1;
        String candidate;
        do {
            candidate = String.format("SB%06d", seq++);
        } while (savingsAccountRepository.existsByAccountNumber(candidate));
        return candidate;
    }

    /**
     * Records a deposit against the account's own ledger (balance + transaction history) without
     * posting to the general ledger. Used by {@link DepositService} when a single combined journal
     * entry is being built for a whole teller receipt.
     */
    @Transactional
    public SavingsTransaction recordDeposit(SavingsAccount account, BigDecimal amount, LocalDate date,
                                             String narration, String createdBy) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Deposit amount must be greater than zero");
        }
        account.setBalance(account.getBalance().add(amount));
        savingsAccountRepository.save(account);

        SavingsTransaction txn = new SavingsTransaction();
        txn.setSavingsAccount(account);
        txn.setTransactionType(SavingsTransactionType.DEPOSIT);
        txn.setAmount(amount);
        txn.setRunningBalance(account.getBalance());
        txn.setTransactionDate(date != null ? date : LocalDate.now());
        txn.setNarration(narration);
        txn.setCreatedBy(createdBy);
        return savingsTransactionRepository.save(txn);
    }

    /** Stand-alone savings deposit (not part of a combined deposit screen entry): also posts the GL entry. */
    @Transactional
    public SavingsTransaction depositWithPosting(SavingsAccount account, BigDecimal amount, LocalDate date,
                                                  String narration, String createdBy) {
        SavingsTransaction txn = recordDeposit(account, amount, date, narration, createdBy);

        GLAccount cash = accountingService.getAccountByCode(GLCodes.CASH_AND_BANK);
        GLAccount savingsControl = accountingService.getAccountByCode(GLCodes.MEMBER_SAVINGS);

        var entry = accountingService.post(txn.getTransactionDate(), "SAVINGS_DEPOSIT", txn.getId(),
                "Savings deposit - " + account.getAccountNumber() + (narration != null ? " - " + narration : ""),
                createdBy,
                List.of(
                        JournalEntryLine.debit(cash, amount, "Cash received"),
                        JournalEntryLine.credit(savingsControl, amount, "Deposit to " + account.getAccountNumber())
                ));
        txn.setJournalEntryId(entry.getId());
        return savingsTransactionRepository.save(txn);
    }

    @Transactional
    public SavingsTransaction withdraw(SavingsAccount account, BigDecimal amount, LocalDate date,
                                        String narration, String createdBy) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Withdrawal amount must be greater than zero");
        }
        if (account.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("Insufficient savings balance for this withdrawal");
        }
        account.setBalance(account.getBalance().subtract(amount));
        savingsAccountRepository.save(account);

        SavingsTransaction txn = new SavingsTransaction();
        txn.setSavingsAccount(account);
        txn.setTransactionType(SavingsTransactionType.WITHDRAWAL);
        txn.setAmount(amount);
        txn.setRunningBalance(account.getBalance());
        txn.setTransactionDate(date != null ? date : LocalDate.now());
        txn.setNarration(narration);
        txn.setCreatedBy(createdBy);
        savingsTransactionRepository.save(txn);

        GLAccount cash = accountingService.getAccountByCode(GLCodes.CASH_AND_BANK);
        GLAccount savingsControl = accountingService.getAccountByCode(GLCodes.MEMBER_SAVINGS);

        var entry = accountingService.post(txn.getTransactionDate(), "SAVINGS_WITHDRAWAL", txn.getId(),
                "Savings withdrawal - " + account.getAccountNumber() + (narration != null ? " - " + narration : ""),
                createdBy,
                List.of(
                        JournalEntryLine.debit(savingsControl, amount, "Withdrawal from " + account.getAccountNumber()),
                        JournalEntryLine.credit(cash, amount, "Cash paid out")
                ));
        txn.setJournalEntryId(entry.getId());
        return savingsTransactionRepository.save(txn);
    }
}
