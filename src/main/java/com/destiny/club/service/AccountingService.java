package com.destiny.club.service;

import com.destiny.club.domain.accounting.*;
import com.destiny.club.dto.report.*;
import com.destiny.club.exception.BusinessException;
import com.destiny.club.exception.NotFoundException;
import com.destiny.club.repository.DepositTransactionRepository;
import com.destiny.club.repository.GLAccountRepository;
import com.destiny.club.repository.JournalEntryLineRepository;
import com.destiny.club.repository.JournalEntryRepository;
import com.destiny.club.repository.LoanTransactionRepository;
import com.destiny.club.repository.SavingsTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Central posting engine and reporting service for the general ledger. Every financial
 * transaction in the system (savings deposit/withdrawal, loan disbursement/repayment,
 * manual journals) is recorded here as a single balanced {@link JournalEntry} so that
 * total debits always equal total credits.
 */
@Service
@RequiredArgsConstructor
public class AccountingService {

    private final GLAccountRepository glAccountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;
    private final SavingsTransactionRepository savingsTransactionRepository;
    private final LoanTransactionRepository loanTransactionRepository;
    private final DepositTransactionRepository depositTransactionRepository;

    private static final DateTimeFormatter REF_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    public GLAccount getAccountByCode(String code) {
        return glAccountRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("GL account not configured: " + code));
    }

    public List<GLAccount> findAllAccounts() {
        return glAccountRepository.findAllByOrderByCode();
    }

    /** Active payment-channel accounts (tills, bank accounts, mobile money, ...) tellers can post cash to/from. */
    public List<GLAccount> findCashAccounts() {
        return glAccountRepository.findAllByOrderByCode().stream()
                .filter(a -> a.isCashAccount() && a.isActive())
                .toList();
    }

    public GLAccount getCashAccountById(Long id) {
        GLAccount account = glAccountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("GL account not found: " + id));
        if (!account.isCashAccount()) {
            throw new BusinessException(account.getName() + " is not configured as a payment channel account");
        }
        return account;
    }

    /**
     * Posts a balanced journal entry. Throws {@link BusinessException} if the lines do not
     * balance, guaranteeing every posting keeps the ledger in double-entry balance.
     */
    @Transactional
    public JournalEntry post(LocalDate transactionDate, String sourceType, Long sourceId,
                              String description, String createdBy, List<JournalEntryLine> lines) {
        if (lines == null || lines.size() < 2) {
            throw new BusinessException("A journal entry needs at least two lines");
        }

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (JournalEntryLine line : lines) {
            if (line.getAmount() == null || line.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Journal line amounts must be greater than zero");
            }
            if (line.getEntryType() == EntryType.DEBIT) {
                totalDebit = totalDebit.add(line.getAmount());
            } else {
                totalCredit = totalCredit.add(line.getAmount());
            }
        }

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BusinessException("Journal entry is not balanced: debit " + totalDebit + " vs credit " + totalCredit);
        }

        JournalEntry entry = new JournalEntry();
        entry.setReference(sourceType + "-" + java.time.LocalDateTime.now().format(REF_FORMAT) + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        entry.setTransactionDate(transactionDate);
        entry.setDescription(description);
        entry.setSourceType(sourceType);
        entry.setSourceId(sourceId);
        entry.setCreatedBy(createdBy);
        lines.forEach(entry::addLine);

        return journalEntryRepository.save(entry);
    }

    /**
     * Posts a new journal entry that exactly reverses an existing one (every line's debit/credit
     * flipped, same accounts and amounts), so that voiding a transaction never rewrites or deletes
     * the original posting - it stays in the ledger, offset by this correcting entry, preserving a
     * full audit trail.
     */
    @Transactional
    public JournalEntry reverseEntry(Long originalEntryId, LocalDate reversalDate, String reason, String createdBy) {
        JournalEntry original = journalEntryRepository.findById(originalEntryId)
                .orElseThrow(() -> new NotFoundException("Journal entry not found: " + originalEntryId));

        List<JournalEntryLine> reversedLines = new ArrayList<>();
        for (JournalEntryLine line : original.getLines()) {
            JournalEntryLine reversed = line.getEntryType() == EntryType.CREDIT
                    ? JournalEntryLine.debit(line.getGlAccount(), line.getAmount(), line.getNarration())
                    : JournalEntryLine.credit(line.getGlAccount(), line.getAmount(), line.getNarration());
            reversed.setClientId(line.getClientId());
            reversedLines.add(reversed);
        }

        String description = "Reversal of " + original.getReference() + (reason != null && !reason.isBlank() ? " - " + reason : "");
        return post(reversalDate, "REVERSAL", original.getId(), description, createdBy, reversedLines);
    }

    public List<JournalEntry> recentEntries() {
        return journalEntryRepository.findRecent();
    }

    // ---------------------------------------------------------------- Trial balance

    @Transactional(readOnly = true)
    public TrialBalanceReport generateTrialBalance(LocalDate asOfDate) {
        Map<Long, BigDecimal> debits = new HashMap<>();
        Map<Long, BigDecimal> credits = new HashMap<>();
        for (JournalEntryLineRepository.AccountEntrySum sum : journalEntryLineRepository.sumByAccountUpTo(asOfDate)) {
            if (sum.getEntryType() == EntryType.DEBIT) {
                debits.put(sum.getAccountId(), sum.getTotal());
            } else {
                credits.put(sum.getAccountId(), sum.getTotal());
            }
        }

        List<TrialBalanceLine> lines = new ArrayList<>();
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (GLAccount account : glAccountRepository.findAllByOrderByCode()) {
            BigDecimal debit = debits.getOrDefault(account.getId(), BigDecimal.ZERO);
            BigDecimal credit = credits.getOrDefault(account.getId(), BigDecimal.ZERO);
            if (debit.compareTo(BigDecimal.ZERO) == 0 && credit.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            BigDecimal net = debit.subtract(credit);
            BigDecimal lineDebit = net.signum() >= 0 ? net : BigDecimal.ZERO;
            BigDecimal lineCredit = net.signum() < 0 ? net.negate() : BigDecimal.ZERO;
            lines.add(new TrialBalanceLine(account.getCode(), account.getName(), account.getAccountType().name(), lineDebit, lineCredit));
            totalDebit = totalDebit.add(lineDebit);
            totalCredit = totalCredit.add(lineCredit);
        }

        return new TrialBalanceReport(asOfDate, lines, totalDebit, totalCredit);
    }

    // ---------------------------------------------------------------- Balance sheet

    @Transactional(readOnly = true)
    public BalanceSheetReport generateBalanceSheet(LocalDate asOfDate) {
        Map<Long, BigDecimal> debits = new HashMap<>();
        Map<Long, BigDecimal> credits = new HashMap<>();
        for (JournalEntryLineRepository.AccountEntrySum sum : journalEntryLineRepository.sumByAccountUpTo(asOfDate)) {
            if (sum.getEntryType() == EntryType.DEBIT) {
                debits.put(sum.getAccountId(), sum.getTotal());
            } else {
                credits.put(sum.getAccountId(), sum.getTotal());
            }
        }

        List<LineItem> assets = new ArrayList<>();
        List<LineItem> liabilities = new ArrayList<>();
        List<LineItem> equity = new ArrayList<>();
        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        BigDecimal totalEquity = BigDecimal.ZERO;
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (GLAccount account : glAccountRepository.findAllByOrderByCode()) {
            BigDecimal debit = debits.getOrDefault(account.getId(), BigDecimal.ZERO);
            BigDecimal credit = credits.getOrDefault(account.getId(), BigDecimal.ZERO);
            switch (account.getAccountType()) {
                case ASSET -> {
                    BigDecimal balance = debit.subtract(credit);
                    if (balance.compareTo(BigDecimal.ZERO) != 0) {
                        assets.add(new LineItem(account.getCode(), account.getName(), balance));
                        totalAssets = totalAssets.add(balance);
                    }
                }
                case LIABILITY -> {
                    BigDecimal balance = credit.subtract(debit);
                    if (balance.compareTo(BigDecimal.ZERO) != 0) {
                        liabilities.add(new LineItem(account.getCode(), account.getName(), balance));
                        totalLiabilities = totalLiabilities.add(balance);
                    }
                }
                case EQUITY -> {
                    BigDecimal balance = credit.subtract(debit);
                    if (balance.compareTo(BigDecimal.ZERO) != 0) {
                        equity.add(new LineItem(account.getCode(), account.getName(), balance));
                        totalEquity = totalEquity.add(balance);
                    }
                }
                case INCOME -> totalIncome = totalIncome.add(credit.subtract(debit));
                case EXPENSE -> totalExpense = totalExpense.add(debit.subtract(credit));
            }
        }

        BigDecimal netSurplus = totalIncome.subtract(totalExpense);

        return new BalanceSheetReport(asOfDate, assets, liabilities, equity, netSurplus, totalAssets, totalLiabilities, totalEquity);
    }

    // ---------------------------------------------------------------- Income & expenditure

    @Transactional(readOnly = true)
    public IncomeStatementReport generateIncomeStatement(LocalDate fromDate, LocalDate toDate) {
        Map<Long, BigDecimal> debits = new HashMap<>();
        Map<Long, BigDecimal> credits = new HashMap<>();
        for (JournalEntryLineRepository.AccountEntrySum sum : journalEntryLineRepository.sumByAccountBetween(fromDate, toDate)) {
            if (sum.getEntryType() == EntryType.DEBIT) {
                debits.put(sum.getAccountId(), sum.getTotal());
            } else {
                credits.put(sum.getAccountId(), sum.getTotal());
            }
        }

        List<LineItem> income = new ArrayList<>();
        List<LineItem> expenses = new ArrayList<>();
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (GLAccount account : glAccountRepository.findAllByOrderByCode()) {
            BigDecimal debit = debits.getOrDefault(account.getId(), BigDecimal.ZERO);
            BigDecimal credit = credits.getOrDefault(account.getId(), BigDecimal.ZERO);
            if (account.getAccountType() == AccountType.INCOME) {
                BigDecimal balance = credit.subtract(debit);
                if (balance.compareTo(BigDecimal.ZERO) != 0) {
                    income.add(new LineItem(account.getCode(), account.getName(), balance));
                    totalIncome = totalIncome.add(balance);
                }
            } else if (account.getAccountType() == AccountType.EXPENSE) {
                BigDecimal balance = debit.subtract(credit);
                if (balance.compareTo(BigDecimal.ZERO) != 0) {
                    expenses.add(new LineItem(account.getCode(), account.getName(), balance));
                    totalExpenses = totalExpenses.add(balance);
                }
            }
        }

        return new IncomeStatementReport(fromDate, toDate, income, expenses, totalIncome, totalExpenses);
    }

    // ---------------------------------------------------------------- General ledger

    @Transactional(readOnly = true)
    public GeneralLedgerReport generateGeneralLedger(Long accountId, LocalDate fromDate, LocalDate toDate) {
        GLAccount account = glAccountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("GL account not found: " + accountId));

        BigDecimal opening = BigDecimal.ZERO;
        if (fromDate != null) {
            LocalDate dayBefore = fromDate.minusDays(1);
            for (JournalEntryLineRepository.AccountEntrySum sum : journalEntryLineRepository.sumByAccountUpTo(dayBefore)) {
                if (!sum.getAccountId().equals(accountId)) {
                    continue;
                }
                BigDecimal signedAmount = account.getAccountType().normalBalanceIsDebit()
                        ? (sum.getEntryType() == EntryType.DEBIT ? sum.getTotal() : sum.getTotal().negate())
                        : (sum.getEntryType() == EntryType.CREDIT ? sum.getTotal() : sum.getTotal().negate());
                opening = opening.add(signedAmount);
            }
        }

        List<JournalEntryLine> lines = journalEntryLineRepository.findLedgerLines(accountId, fromDate, toDate);
        List<LedgerRow> rows = new ArrayList<>();
        BigDecimal running = opening;
        for (JournalEntryLine line : lines) {
            BigDecimal debit = line.getEntryType() == EntryType.DEBIT ? line.getAmount() : BigDecimal.ZERO;
            BigDecimal credit = line.getEntryType() == EntryType.CREDIT ? line.getAmount() : BigDecimal.ZERO;
            BigDecimal signed = account.getAccountType().normalBalanceIsDebit()
                    ? debit.subtract(credit)
                    : credit.subtract(debit);
            running = running.add(signed);
            JournalEntry entry = line.getJournalEntry();
            rows.add(new LedgerRow(
                    entry.getTransactionDate(),
                    entry.getReference(),
                    transactionPersonName(entry),
                    line.getNarration() != null ? line.getNarration() : entry.getDescription(),
                    sourceTypeLabel(entry.getSourceType()),
                    transactionViewUrl(entry),
                    debit, credit, running));
        }

        return new GeneralLedgerReport(account, fromDate, toDate, opening, rows, running);
    }

    /** Plain-English label for a journal entry's source type, for the General Ledger's Type column. */
    private String sourceTypeLabel(String sourceType) {
        if (sourceType == null) {
            return "-";
        }
        return switch (sourceType) {
            case "SAVINGS_DEPOSIT" -> "Savings Deposit";
            case "SAVINGS_WITHDRAWAL" -> "Savings Withdrawal";
            case "FIELD_COLLECTION" -> "Field Collection";
            case "LOAN_DISBURSEMENT" -> "Loan Disbursement";
            case "LOAN_REPAYMENT" -> "Loan Repayment";
            case "DEPOSIT_TRANSACTION" -> "Combined Deposit";
            case "MANUAL" -> "Manual Entry";
            case "REVERSAL" -> "Reversal";
            default -> {
                String lower = sourceType.toLowerCase().replace('_', ' ');
                yield Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
            }
        };
    }

    /**
     * Where "View" on a General Ledger row should take you - the specific savings/loan
     * transaction or deposit that actually created this entry (with its own Edit/Void actions),
     * falling back to the journal entry itself for manual entries, reversals, or anything else
     * without its own dedicated screen.
     */
    private String transactionViewUrl(JournalEntry entry) {
        Long sourceId = entry.getSourceId();
        if (sourceId != null) {
            switch (entry.getSourceType()) {
                case "SAVINGS_DEPOSIT", "SAVINGS_WITHDRAWAL", "FIELD_COLLECTION" -> {
                    return "/savings-accounts/transactions/" + sourceId;
                }
                case "LOAN_DISBURSEMENT", "LOAN_REPAYMENT" -> {
                    return "/loan-accounts/transactions/" + sourceId;
                }
                case "DEPOSIT_TRANSACTION" -> {
                    return "/deposits/" + sourceId;
                }
                default -> {
                    // fall through to the journal entry itself
                }
            }
        }
        return "/journal/" + entry.getId();
    }

    /**
     * The member/group a General Ledger entry was posted for, looked up from whichever
     * transaction actually created it - so the ledger reads as "Jane Mukasa" rather than a
     * machine-generated reference code. A reversal traces back to the person on the entry it
     * reversed. Null for entries with no single owner (manual journals).
     */
    private String transactionPersonName(JournalEntry entry) {
        Long sourceId = entry.getSourceId();
        if (sourceId == null) {
            return null;
        }
        return switch (entry.getSourceType()) {
            case "SAVINGS_DEPOSIT", "SAVINGS_WITHDRAWAL", "FIELD_COLLECTION" -> savingsTransactionRepository.findById(sourceId)
                    .map(t -> t.getSavingsAccount().getOwnerName())
                    .orElse(null);
            case "LOAN_DISBURSEMENT", "LOAN_REPAYMENT" -> loanTransactionRepository.findById(sourceId)
                    .map(t -> t.getLoanAccount().getBorrowerName())
                    .orElse(null);
            case "DEPOSIT_TRANSACTION" -> depositTransactionRepository.findById(sourceId)
                    .map(d -> d.getClient() != null ? d.getClient().getFullName()
                            : (d.getGroup() != null ? d.getGroup().getGroupName() : null))
                    .orElse(null);
            case "REVERSAL" -> journalEntryRepository.findById(sourceId)
                    .map(this::transactionPersonName)
                    .orElse(null);
            default -> null;
        };
    }
}
