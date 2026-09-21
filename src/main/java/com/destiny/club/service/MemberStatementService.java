package com.destiny.club.service;

import com.destiny.club.domain.client.Client;
import com.destiny.club.domain.loan.LoanAccount;
import com.destiny.club.domain.loan.LoanTransaction;
import com.destiny.club.domain.loan.LoanTransactionType;
import com.destiny.club.domain.savings.SavingsAccount;
import com.destiny.club.domain.savings.SavingsTransaction;
import com.destiny.club.domain.savings.SavingsTransactionType;
import com.destiny.club.dto.report.MemberStatementReport;
import com.destiny.club.dto.report.MemberStatementRow;
import com.destiny.club.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a single chronological statement for one member covering both their savings
 * movements and their loan movements (disbursements and repayments), with a running balance
 * for each side carried on every row - the client-facing "member statement" report.
 */
@Service
@RequiredArgsConstructor
public class MemberStatementService {

    private final ClientService clientService;
    private final SavingsAccountRepository savingsAccountRepository;
    private final SavingsTransactionRepository savingsTransactionRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final LoanTransactionRepository loanTransactionRepository;
    private final LoanRepaymentInstallmentRepository loanRepaymentInstallmentRepository;
    private final JournalEntryRepository journalEntryRepository;

    @Transactional(readOnly = true)
    public MemberStatementReport generate(Long clientId, LocalDate fromDate, LocalDate toDate) {
        Client client = clientService.getById(clientId);
        Map<Long, String> referenceCache = new HashMap<>();

        List<SavingsAccount> savingsAccounts = savingsAccountRepository.findByClientId(clientId);
        List<LoanAccount> loanAccounts = loanAccountRepository.findByClientId(clientId);

        List<MemberStatementRow> savingsRows = new ArrayList<>();
        BigDecimal openingSavings = BigDecimal.ZERO;
        for (SavingsAccount account : savingsAccounts) {
            List<SavingsTransaction> txns = savingsTransactionRepository
                    .findBySavingsAccountIdOrderByTransactionDateDescIdDesc(account.getId());
            txns.sort(Comparator.comparing(SavingsTransaction::getTransactionDate).thenComparing(SavingsTransaction::getId));

            BigDecimal accountOpening = BigDecimal.ZERO;
            for (SavingsTransaction txn : txns) {
                if (!txn.getTransactionDate().isBefore(fromDate)) {
                    break;
                }
                accountOpening = txn.getRunningBalance();
            }
            openingSavings = openingSavings.add(accountOpening);

            for (SavingsTransaction txn : txns) {
                if (txn.getTransactionDate().isBefore(fromDate) || txn.getTransactionDate().isAfter(toDate)) {
                    continue;
                }
                BigDecimal signedAmount = txn.getTransactionType() == SavingsTransactionType.WITHDRAWAL
                        ? txn.getAmount().negate() : txn.getAmount();
                String description = "Savings " + humanize(txn.getTransactionType().name())
                        + (txn.getNarration() != null ? " - " + txn.getNarration() : "");
                savingsRows.add(new MemberStatementRow(txn.getTransactionDate(), description,
                        referenceFor(txn.getJournalEntryId(), referenceCache), account.getAccountNumber(),
                        signedAmount, null, null, null));
            }
        }

        List<MemberStatementRow> loanRows = new ArrayList<>();
        BigDecimal openingLoan = BigDecimal.ZERO;
        for (LoanAccount loan : loanAccounts) {
            BigDecimal totalScheduledInterest = loanRepaymentInstallmentRepository
                    .findByLoanAccountIdOrderByInstallmentNumberAsc(loan.getId()).stream()
                    .map(i -> i.getInterestDue())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<LoanTransaction> txns = loanTransactionRepository
                    .findByLoanAccountIdOrderByTransactionDateDescIdDesc(loan.getId());
            txns.sort(Comparator.comparing(LoanTransaction::getTransactionDate).thenComparing(LoanTransaction::getId));

            BigDecimal runningLoanBalance = BigDecimal.ZERO;
            BigDecimal accountOpening = BigDecimal.ZERO;

            for (LoanTransaction txn : txns) {
                BigDecimal delta = loanBalanceDelta(txn, totalScheduledInterest);

                if (txn.getTransactionDate().isBefore(fromDate)) {
                    runningLoanBalance = runningLoanBalance.add(delta);
                    accountOpening = runningLoanBalance;
                    continue;
                }
                if (txn.getTransactionDate().isAfter(toDate)) {
                    continue;
                }

                String description = "Loan " + humanize(txn.getTransactionType().name()) + " - " + loan.getLoanAccountNumber()
                        + (txn.getNarration() != null ? " - " + txn.getNarration() : "");
                loanRows.add(new MemberStatementRow(txn.getTransactionDate(), description,
                        referenceFor(txn.getJournalEntryId(), referenceCache), loan.getLoanAccountNumber(),
                        null, null, delta, null));
            }
            openingLoan = openingLoan.add(accountOpening);
        }

        // Merge both sides onto one timeline. Each row carries a delta on whichever side it belongs to
        // (savingsAmount or loanAmount); the running totals below accumulate those deltas across ALL of
        // the member's savings accounts and ALL of their loans combined, and every row shows both current
        // totals regardless of which side actually moved on that row.
        List<MemberStatementRow> merged = new ArrayList<>(savingsRows.size() + loanRows.size());
        merged.addAll(savingsRows);
        merged.addAll(loanRows);
        merged.sort(Comparator.comparing(MemberStatementRow::getDate));

        List<MemberStatementRow> finalRows = new ArrayList<>(merged.size());
        BigDecimal runningSavings = openingSavings;
        BigDecimal runningLoan = openingLoan;
        for (MemberStatementRow row : merged) {
            if (row.getSavingsAmount() != null) {
                runningSavings = runningSavings.add(row.getSavingsAmount());
            }
            if (row.getLoanAmount() != null) {
                runningLoan = runningLoan.add(row.getLoanAmount());
            }
            finalRows.add(new MemberStatementRow(row.getDate(), row.getDescription(), row.getReference(),
                    row.getAccountNumber(), row.getSavingsAmount(), runningSavings, row.getLoanAmount(), runningLoan));
        }

        return new MemberStatementReport(client, fromDate, toDate, openingSavings, openingLoan,
                finalRows, runningSavings, runningLoan);
    }

    /** Signed change in total loan balance owed: principal (+scheduled interest) on disbursement, negative on repayment. */
    private BigDecimal loanBalanceDelta(LoanTransaction txn, BigDecimal totalScheduledInterest) {
        if (txn.getTransactionType() == LoanTransactionType.DISBURSEMENT) {
            return txn.getPrincipalPortion().add(totalScheduledInterest);
        }
        if (txn.getTransactionType() == LoanTransactionType.REPAYMENT) {
            return txn.getAmount().negate();
        }
        if (txn.getTransactionType() == LoanTransactionType.WRITE_OFF) {
            return txn.getAmount().negate();
        }
        return BigDecimal.ZERO;
    }

    private String referenceFor(Long journalEntryId, Map<Long, String> referenceCache) {
        if (journalEntryId == null) {
            return "-";
        }
        return referenceCache.computeIfAbsent(journalEntryId,
                id -> journalEntryRepository.findById(id).map(je -> je.getReference()).orElse("-"));
    }

    private String humanize(String enumName) {
        String lower = enumName.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
