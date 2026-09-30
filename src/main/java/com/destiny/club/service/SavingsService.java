package com.destiny.club.service;

import com.destiny.club.domain.accounting.GLAccount;
import com.destiny.club.domain.accounting.GLCodes;
import com.destiny.club.domain.accounting.JournalEntryLine;
import com.destiny.club.domain.client.Client;
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
import java.time.LocalDateTime;
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

    public List<SavingsTransaction> transactionsFor(Long savingsAccountId) {
        return savingsTransactionRepository.findBySavingsAccountIdOrderByTransactionDateDescIdDesc(savingsAccountId);
    }

    /** All savings transactions (deposits and withdrawals, across every account) within a date range, oldest first. */
    public List<SavingsTransaction> findTransactionsBetween(LocalDate fromDate, LocalDate toDate) {
        return savingsTransactionRepository.findByTransactionDateBetweenOrderByTransactionDateAscIdAsc(fromDate, toDate);
    }

    @Transactional
    public SavingsAccount openAccount(Client client, SavingsProduct product, LocalDate openedDate) {
        if (client == null) {
            throw new BusinessException("A savings account must belong to a client");
        }
        SavingsAccount account = new SavingsAccount();
        account.setClient(client);
        account.setSavingsProduct(product);
        account.setOpenedDate(openedDate != null ? openedDate : LocalDate.now());
        account.setAccountNumber(generateAccountNumber());
        account.setBalance(BigDecimal.ZERO);
        return savingsAccountRepository.save(account);
    }

    /**
     * Returns the client's existing active savings account under the given product, or silently
     * opens a new one if they don't have one yet. Used by the Savings Deposit screen so that a
     * teller never has to "open an account" as a separate step - the first deposit under a
     * product opens it automatically.
     */
    @Transactional
    public SavingsAccount findOrOpenAccount(Client client, SavingsProduct product) {
        List<SavingsAccount> existing = findByClient(client.getId());
        return existing.stream()
                .filter(a -> a.getSavingsProduct().getId().equals(product.getId()) && a.getStatus() == SavingsAccountStatus.ACTIVE)
                .findFirst()
                .orElseGet(() -> openAccount(client, product, LocalDate.now()));
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
                                                  String narration, String createdBy, GLAccount cashAccount) {
        SavingsTransaction txn = recordDeposit(account, amount, date, narration, createdBy);

        GLAccount savingsControl = accountingService.getAccountByCode(GLCodes.MEMBER_SAVINGS);

        var entry = accountingService.post(txn.getTransactionDate(), "SAVINGS_DEPOSIT", txn.getId(),
                "Savings deposit - " + account.getAccountNumber() + (narration != null ? " - " + narration : ""),
                createdBy,
                List.of(
                        JournalEntryLine.debit(cashAccount, amount, "Cash received"),
                        JournalEntryLine.credit(savingsControl, amount, "Deposit to " + account.getAccountNumber())
                ));
        txn.setJournalEntryId(entry.getId());
        return savingsTransactionRepository.save(txn);
    }

    @Transactional
    public SavingsTransaction withdraw(SavingsAccount account, BigDecimal amount, LocalDate date,
                                        String narration, String createdBy, GLAccount cashAccount) {
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

        GLAccount savingsControl = accountingService.getAccountByCode(GLCodes.MEMBER_SAVINGS);

        var entry = accountingService.post(txn.getTransactionDate(), "SAVINGS_WITHDRAWAL", txn.getId(),
                "Savings withdrawal - " + account.getAccountNumber() + (narration != null ? " - " + narration : ""),
                createdBy,
                List.of(
                        JournalEntryLine.debit(savingsControl, amount, "Withdrawal from " + account.getAccountNumber()),
                        JournalEntryLine.credit(cashAccount, amount, "Cash paid out")
                ));
        txn.setJournalEntryId(entry.getId());
        return savingsTransactionRepository.save(txn);
    }

    public List<SavingsTransaction> findByDepositTransaction(Long depositTransactionId) {
        return savingsTransactionRepository.findByDepositTransactionId(depositTransactionId);
    }

    public SavingsTransaction getTransaction(Long transactionId) {
        return savingsTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Savings transaction not found: " + transactionId));
    }

    @Transactional
    public SavingsTransaction updateNarration(Long transactionId, String narration, String updatedBy) {
        SavingsTransaction txn = getTransaction(transactionId);
        if (txn.isVoided()) {
            throw new BusinessException("Cannot edit a voided transaction");
        }
        txn.setNarration(narration);
        return savingsTransactionRepository.save(txn);
    }

    /**
     * Voids a stand-alone savings transaction (one with its own journal entry): posts a
     * reversing journal entry, then recomputes the account's balance and every later
     * transaction's running balance from its remaining non-voided history.
     */
    @Transactional
    public SavingsTransaction voidTransaction(Long transactionId, String voidedBy, String reason) {
        SavingsTransaction txn = getTransaction(transactionId);
        if (txn.isVoided()) {
            throw new BusinessException("This transaction has already been voided");
        }
        if (txn.getDepositTransactionId() != null) {
            throw new BusinessException("This transaction is part of a combined deposit - void the deposit instead");
        }
        if (txn.getJournalEntryId() != null) {
            var reversal = accountingService.reverseEntry(txn.getJournalEntryId(), LocalDate.now(), reason, voidedBy);
            txn.setReversalJournalEntryId(reversal.getId());
        }
        markVoided(txn, voidedBy, reason);
        recomputeAccount(txn.getSavingsAccount());
        return txn;
    }

    /**
     * Voids the savings leg of a combined deposit. No journal reversal happens here - the parent
     * deposit posts one combined reversing entry covering every leg (savings, shares, loan) at once.
     */
    @Transactional
    public SavingsTransaction voidSubTransaction(Long transactionId, String voidedBy, String reason) {
        SavingsTransaction txn = getTransaction(transactionId);
        if (txn.isVoided()) {
            throw new BusinessException("This transaction has already been voided");
        }
        markVoided(txn, voidedBy, reason);
        recomputeAccount(txn.getSavingsAccount());
        return txn;
    }

    private void markVoided(SavingsTransaction txn, String voidedBy, String reason) {
        txn.setVoided(true);
        txn.setVoidedBy(voidedBy);
        txn.setVoidedAt(LocalDateTime.now());
        txn.setVoidReason(reason);
        savingsTransactionRepository.save(txn);
    }

    /**
     * Recomputes the account's balance and every transaction's running balance by replaying its
     * non-voided history in date order - so voiding any past transaction (not just the latest)
     * still leaves every later balance correct.
     */
    @Transactional
    public void recomputeAccount(SavingsAccount account) {
        List<SavingsTransaction> txns = savingsTransactionRepository
                .findBySavingsAccountIdOrderByTransactionDateAscIdAsc(account.getId());
        BigDecimal balance = BigDecimal.ZERO;
        for (SavingsTransaction txn : txns) {
            if (txn.isVoided()) {
                continue;
            }
            balance = txn.getTransactionType() == SavingsTransactionType.DEPOSIT
                    ? balance.add(txn.getAmount())
                    : balance.subtract(txn.getAmount());
            txn.setRunningBalance(balance);
            savingsTransactionRepository.save(txn);
        }
        account.setBalance(balance);
        savingsAccountRepository.save(account);
    }
}
