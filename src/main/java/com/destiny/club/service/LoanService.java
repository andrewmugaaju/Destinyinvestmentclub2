package com.destiny.club.service;

import com.destiny.club.domain.accounting.GLAccount;
import com.destiny.club.domain.accounting.GLCodes;
import com.destiny.club.domain.accounting.JournalEntryLine;
import com.destiny.club.domain.client.Client;
import com.destiny.club.domain.client.Group;
import com.destiny.club.domain.loan.*;
import com.destiny.club.exception.BusinessException;
import com.destiny.club.exception.NotFoundException;
import com.destiny.club.repository.LoanAccountRepository;
import com.destiny.club.repository.LoanRepaymentInstallmentRepository;
import com.destiny.club.repository.LoanTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the loan lifecycle: application, approval, disbursement (with a declining-balance,
 * equal-installment amortization schedule) and repayment, all reconciled to the general ledger.
 */
@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanAccountRepository loanAccountRepository;
    private final LoanRepaymentInstallmentRepository installmentRepository;
    private final LoanTransactionRepository loanTransactionRepository;
    private final AccountingService accountingService;

    public List<LoanAccount> findAll() {
        return loanAccountRepository.findAll();
    }

    public LoanAccount getById(Long id) {
        return loanAccountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Loan account not found: " + id));
    }

    public List<LoanAccount> findByClient(Long clientId) {
        return loanAccountRepository.findByClientId(clientId);
    }

    public List<LoanAccount> findByGroup(Long groupId) {
        return loanAccountRepository.findByGroupId(groupId);
    }

    public List<LoanAccount> findActiveByClient(Long clientId) {
        return loanAccountRepository.findByClientIdAndStatus(clientId, LoanStatus.ACTIVE);
    }

    public List<LoanAccount> findActiveByGroup(Long groupId) {
        return loanAccountRepository.findByGroupIdAndStatus(groupId, LoanStatus.ACTIVE);
    }

    public List<LoanTransaction> transactionsFor(Long loanAccountId) {
        return loanTransactionRepository.findByLoanAccountIdOrderByTransactionDateDescIdDesc(loanAccountId);
    }

    /** All loan transactions (disbursements and repayments, across every loan) within a date range, oldest first. */
    public List<LoanTransaction> findTransactionsBetween(LocalDate fromDate, LocalDate toDate) {
        return loanTransactionRepository.findByTransactionDateBetweenOrderByTransactionDateAscIdAsc(fromDate, toDate);
    }

    @Transactional
    public LoanAccount apply(Client client, Group group, LoanProduct product, BigDecimal principal, Integer termMonths,
                              BigDecimal applicationFeeAmount) {
        if (client == null && group == null) {
            throw new BusinessException("A loan must belong to a client or a group");
        }
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Loan principal must be greater than zero");
        }
        BigDecimal fee = applicationFeeAmount != null ? applicationFeeAmount : product.getApplicationFeeAmount();
        if (fee.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Application fee cannot be negative");
        }
        if (fee.compareTo(principal) >= 0) {
            throw new BusinessException("Application fee must be less than the loan principal");
        }
        LoanAccount loan = new LoanAccount();
        loan.setClient(client);
        loan.setGroup(group);
        loan.setLoanProduct(product);
        loan.setPrincipalAmount(principal);
        loan.setAnnualInterestRate(product.getAnnualInterestRate());
        loan.setTermMonths(termMonths != null ? termMonths : product.getDefaultTermMonths());
        loan.setApplicationFeeAmount(fee);
        loan.setApplicationDate(LocalDate.now());
        loan.setStatus(LoanStatus.PENDING);
        loan.setLoanAccountNumber(generateLoanAccountNumber());
        return loanAccountRepository.save(loan);
    }

    private String generateLoanAccountNumber() {
        long seq = loanAccountRepository.count() + 1;
        String candidate;
        do {
            candidate = String.format("LN%06d", seq++);
        } while (loanAccountRepository.existsByLoanAccountNumber(candidate));
        return candidate;
    }

    @Transactional
    public LoanAccount approve(Long loanId, String approvedBy) {
        LoanAccount loan = getById(loanId);
        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new BusinessException("Only pending loans can be approved");
        }
        loan.setStatus(LoanStatus.APPROVED);
        loan.setApprovedBy(approvedBy);
        return loanAccountRepository.save(loan);
    }

    @Transactional
    public LoanAccount reject(Long loanId, String rejectedBy) {
        LoanAccount loan = getById(loanId);
        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new BusinessException("Only pending loans can be rejected");
        }
        loan.setStatus(LoanStatus.REJECTED);
        loan.setApprovedBy(rejectedBy);
        return loanAccountRepository.save(loan);
    }

    @Transactional
    public LoanAccount disburse(Long loanId, LocalDate disbursementDate, String createdBy, GLAccount cashAccount) {
        LoanAccount loan = getById(loanId);
        if (loan.getStatus() != LoanStatus.APPROVED) {
            throw new BusinessException("Only approved loans can be disbursed");
        }

        LocalDate date = disbursementDate != null ? disbursementDate : LocalDate.now();
        loan.setDisbursementDate(date);
        loan.setOutstandingPrincipal(loan.getPrincipalAmount());
        loan.setOutstandingInterest(BigDecimal.ZERO);
        loan.setStatus(LoanStatus.ACTIVE);
        loanAccountRepository.save(loan);

        buildAmortizationSchedule(loan);

        LoanTransaction txn = new LoanTransaction();
        txn.setLoanAccount(loan);
        txn.setTransactionType(LoanTransactionType.DISBURSEMENT);
        txn.setAmount(loan.getPrincipalAmount());
        txn.setPrincipalPortion(loan.getPrincipalAmount());
        txn.setTransactionDate(date);
        txn.setNarration("Loan disbursement - " + loan.getLoanAccountNumber());
        txn.setCreatedBy(createdBy);
        loanTransactionRepository.save(txn);

        GLAccount loansReceivable = accountingService.getAccountByCode(GLCodes.LOANS_RECEIVABLE);

        // The borrower still owes the full principal, but the application fee is netted off the
        // cash actually paid out and recognised immediately as fee income.
        BigDecimal fee = loan.getApplicationFeeAmount();
        BigDecimal netCashOut = loan.getPrincipalAmount().subtract(fee);

        List<JournalEntryLine> lines = new ArrayList<>();
        lines.add(JournalEntryLine.debit(loansReceivable, loan.getPrincipalAmount(), "Disbursed to " + loan.getLoanAccountNumber()));
        lines.add(JournalEntryLine.credit(cashAccount, netCashOut, "Net cash disbursed"));
        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            GLAccount feesIncome = accountingService.getAccountByCode(GLCodes.FEES_AND_CHARGES_INCOME);
            lines.add(JournalEntryLine.credit(feesIncome, fee, "Loan application fee - " + loan.getLoanAccountNumber()));
        }

        var entry = accountingService.post(date, "LOAN_DISBURSEMENT", txn.getId(),
                "Loan disbursement - " + loan.getLoanAccountNumber(), createdBy, lines);
        txn.setJournalEntryId(entry.getId());
        loanTransactionRepository.save(txn);

        return loan;
    }

    /** Declining-balance, equal-installment amortization schedule. */
    private void buildAmortizationSchedule(LoanAccount loan) {
        int n = loan.getTermMonths();
        BigDecimal monthlyRate = loan.getAnnualInterestRate()
                .divide(BigDecimal.valueOf(100), MathContext.DECIMAL64)
                .divide(BigDecimal.valueOf(12), MathContext.DECIMAL64);

        BigDecimal installment;
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            installment = loan.getPrincipalAmount().divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        } else {
            BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
            BigDecimal factor = BigDecimal.ONE.subtract(onePlusR.pow(-n, MathContext.DECIMAL64));
            installment = loan.getPrincipalAmount().multiply(monthlyRate).divide(factor, 2, RoundingMode.HALF_UP);
        }

        BigDecimal remainingPrincipal = loan.getPrincipalAmount();
        List<LoanRepaymentInstallment> schedule = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            BigDecimal interestDue = remainingPrincipal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalDue;
            if (i == n) {
                principalDue = remainingPrincipal.setScale(2, RoundingMode.HALF_UP);
            } else {
                principalDue = installment.subtract(interestDue).setScale(2, RoundingMode.HALF_UP);
            }
            remainingPrincipal = remainingPrincipal.subtract(principalDue);

            LoanRepaymentInstallment inst = new LoanRepaymentInstallment();
            inst.setLoanAccount(loan);
            inst.setInstallmentNumber(i);
            inst.setDueDate(loan.getDisbursementDate().plusMonths(i));
            inst.setPrincipalDue(principalDue);
            inst.setInterestDue(interestDue);
            schedule.add(inst);
        }
        installmentRepository.saveAll(schedule);

        BigDecimal totalInterest = schedule.stream().map(LoanRepaymentInstallment::getInterestDue).reduce(BigDecimal.ZERO, BigDecimal::add);
        loan.setOutstandingInterest(totalInterest);
        loanAccountRepository.save(loan);
    }

    /**
     * Records a loan repayment against the account's own ledger (schedule + balances) without
     * posting to the general ledger. Used by {@link DepositService} when a single combined journal
     * entry is being built for a whole teller receipt. Payment is applied oldest-installment-first,
     * interest before principal within each installment.
     */
    @Transactional
    public LoanTransaction recordRepayment(LoanAccount loan, BigDecimal amount, LocalDate date, String narration, String createdBy) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Repayment amount must be greater than zero");
        }
        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new BusinessException("Repayments can only be recorded against an active loan");
        }

        BigDecimal[] applied = applyRepaymentToInstallments(loan, amount);
        BigDecimal principalApplied = applied[0];
        BigDecimal interestApplied = applied[1];

        loan.setOutstandingInterest(loan.getOutstandingInterest().subtract(interestApplied).max(BigDecimal.ZERO));
        loan.setOutstandingPrincipal(loan.getOutstandingPrincipal().subtract(principalApplied).max(BigDecimal.ZERO));
        if (loan.getOutstandingPrincipal().compareTo(BigDecimal.ZERO) == 0
                && loan.getOutstandingInterest().compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus(LoanStatus.CLOSED);
        }
        loanAccountRepository.save(loan);

        LoanTransaction txn = new LoanTransaction();
        txn.setLoanAccount(loan);
        txn.setTransactionType(LoanTransactionType.REPAYMENT);
        txn.setAmount(amount);
        txn.setPrincipalPortion(principalApplied);
        txn.setInterestPortion(interestApplied);
        txn.setTransactionDate(date != null ? date : LocalDate.now());
        txn.setNarration(narration);
        txn.setCreatedBy(createdBy);
        return loanTransactionRepository.save(txn);
    }

    /**
     * Applies a repayment amount across a loan's installments oldest-first, interest before
     * principal within each installment, then any leftover against outstanding principal. Shared
     * by {@link #recordRepayment} and {@link #recomputeLoanState} (which replays every non-voided
     * repayment through this exact same allocation) so voiding a repayment stays consistent with
     * how repayments were applied in the first place.
     *
     * @return {principalApplied, interestApplied}
     */
    private BigDecimal[] applyRepaymentToInstallments(LoanAccount loan, BigDecimal amount) {
        BigDecimal remaining = amount;
        BigDecimal principalApplied = BigDecimal.ZERO;
        BigDecimal interestApplied = BigDecimal.ZERO;

        List<LoanRepaymentInstallment> installments = installmentRepository.findByLoanAccountIdOrderByInstallmentNumberAsc(loan.getId());
        for (LoanRepaymentInstallment inst : installments) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            if (inst.isFullyPaid()) {
                continue;
            }
            BigDecimal interestOwed = inst.getInterestBalance();
            BigDecimal interestPay = remaining.min(interestOwed);
            inst.setInterestPaid(inst.getInterestPaid().add(interestPay));
            interestApplied = interestApplied.add(interestPay);
            remaining = remaining.subtract(interestPay);

            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal principalOwed = inst.getPrincipalBalance();
                BigDecimal principalPay = remaining.min(principalOwed);
                inst.setPrincipalPaid(inst.getPrincipalPaid().add(principalPay));
                principalApplied = principalApplied.add(principalPay);
                remaining = remaining.subtract(principalPay);
            }

            if (inst.getPrincipalBalance().compareTo(BigDecimal.ZERO) <= 0
                    && inst.getInterestBalance().compareTo(BigDecimal.ZERO) <= 0) {
                inst.setFullyPaid(true);
            }
            installmentRepository.save(inst);
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            // Overpayment beyond scheduled installments: apply against outstanding principal.
            principalApplied = principalApplied.add(remaining);
        }
        return new BigDecimal[] { principalApplied, interestApplied };
    }

    /** Stand-alone loan repayment (not part of a combined deposit screen entry): also posts the GL entry. */
    @Transactional
    public LoanTransaction repayWithPosting(LoanAccount loan, BigDecimal amount, LocalDate date, String narration,
                                             String createdBy, GLAccount cashAccount) {
        LoanTransaction txn = recordRepayment(loan, amount, date, narration, createdBy);

        GLAccount loansReceivable = accountingService.getAccountByCode(GLCodes.LOANS_RECEIVABLE);
        GLAccount interestIncome = accountingService.getAccountByCode(GLCodes.LOAN_INTEREST_INCOME);

        List<JournalEntryLine> lines = new ArrayList<>();
        lines.add(JournalEntryLine.debit(cashAccount, txn.getAmount(), "Cash received"));
        if (txn.getPrincipalPortion().compareTo(BigDecimal.ZERO) > 0) {
            lines.add(JournalEntryLine.credit(loansReceivable, txn.getPrincipalPortion(), "Principal repayment " + loan.getLoanAccountNumber()));
        }
        if (txn.getInterestPortion().compareTo(BigDecimal.ZERO) > 0) {
            lines.add(JournalEntryLine.credit(interestIncome, txn.getInterestPortion(), "Interest repayment " + loan.getLoanAccountNumber()));
        }

        var entry = accountingService.post(txn.getTransactionDate(), "LOAN_REPAYMENT", txn.getId(),
                "Loan repayment - " + loan.getLoanAccountNumber() + (narration != null ? " - " + narration : ""),
                createdBy, lines);
        txn.setJournalEntryId(entry.getId());
        return loanTransactionRepository.save(txn);
    }

    public List<LoanTransaction> findByDepositTransaction(Long depositTransactionId) {
        return loanTransactionRepository.findByDepositTransactionId(depositTransactionId);
    }

    public LoanTransaction getTransaction(Long transactionId) {
        return loanTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Loan transaction not found: " + transactionId));
    }

    @Transactional
    public LoanTransaction updateNarration(Long transactionId, String narration, String updatedBy) {
        LoanTransaction txn = getTransaction(transactionId);
        if (txn.isVoided()) {
            throw new BusinessException("Cannot edit a voided transaction");
        }
        txn.setNarration(narration);
        return loanTransactionRepository.save(txn);
    }

    /**
     * Voids a stand-alone loan transaction (a disbursement or repayment with its own journal
     * entry): posts a reversing journal entry, then recomputes the loan's outstanding balances,
     * status and installment schedule from its remaining non-voided history. A disbursement can
     * only be voided once every repayment made against it has itself been voided first.
     */
    @Transactional
    public LoanTransaction voidTransaction(Long transactionId, String voidedBy, String reason) {
        LoanTransaction txn = getTransaction(transactionId);
        if (txn.isVoided()) {
            throw new BusinessException("This transaction has already been voided");
        }
        if (txn.getDepositTransactionId() != null) {
            throw new BusinessException("This transaction is part of a combined deposit - void the deposit instead");
        }
        LoanAccount loan = txn.getLoanAccount();
        if (txn.getTransactionType() == LoanTransactionType.DISBURSEMENT) {
            boolean hasActiveRepayments = loanTransactionRepository.findByLoanAccountIdOrderByTransactionDateAscIdAsc(loan.getId())
                    .stream().anyMatch(t -> !t.isVoided() && !t.getId().equals(txn.getId())
                            && t.getTransactionType() == LoanTransactionType.REPAYMENT);
            if (hasActiveRepayments) {
                throw new BusinessException("Void the loan's repayments first before voiding its disbursement");
            }
        }
        if (txn.getJournalEntryId() != null) {
            var reversal = accountingService.reverseEntry(txn.getJournalEntryId(), LocalDate.now(), reason, voidedBy);
            txn.setReversalJournalEntryId(reversal.getId());
        }
        markVoided(txn, voidedBy, reason);
        recomputeLoanState(loan);
        return txn;
    }

    /**
     * Voids the loan-repayment leg of a combined deposit. No journal reversal happens here - the
     * parent deposit posts one combined reversing entry covering every leg (savings, shares, loan)
     * at once.
     */
    @Transactional
    public LoanTransaction voidSubTransaction(Long transactionId, String voidedBy, String reason) {
        LoanTransaction txn = getTransaction(transactionId);
        if (txn.isVoided()) {
            throw new BusinessException("This transaction has already been voided");
        }
        LoanAccount loan = txn.getLoanAccount();
        markVoided(txn, voidedBy, reason);
        recomputeLoanState(loan);
        return txn;
    }

    private void markVoided(LoanTransaction txn, String voidedBy, String reason) {
        txn.setVoided(true);
        txn.setVoidedBy(voidedBy);
        txn.setVoidedAt(LocalDateTime.now());
        txn.setVoidReason(reason);
        loanTransactionRepository.save(txn);
    }

    /**
     * Recomputes the loan's outstanding principal/interest, status and every installment's paid
     * amounts by resetting them and replaying every remaining non-voided transaction in date order
     * through the exact same disbursement/repayment logic used when they were first posted - so
     * voiding any past transaction (not just the latest) still leaves the loan's state correct.
     */
    @Transactional
    public void recomputeLoanState(LoanAccount loan) {
        List<LoanRepaymentInstallment> installments = installmentRepository.findByLoanAccountIdOrderByInstallmentNumberAsc(loan.getId());
        for (LoanRepaymentInstallment inst : installments) {
            inst.setPrincipalPaid(BigDecimal.ZERO);
            inst.setInterestPaid(BigDecimal.ZERO);
            inst.setFullyPaid(false);
            installmentRepository.save(inst);
        }

        List<LoanTransaction> txns = loanTransactionRepository.findByLoanAccountIdOrderByTransactionDateAscIdAsc(loan.getId());
        boolean disbursed = txns.stream().anyMatch(t -> !t.isVoided() && t.getTransactionType() == LoanTransactionType.DISBURSEMENT);

        if (!disbursed) {
            loan.setStatus(LoanStatus.APPROVED);
            loan.setOutstandingPrincipal(BigDecimal.ZERO);
            loan.setOutstandingInterest(BigDecimal.ZERO);
            loanAccountRepository.save(loan);
            return;
        }

        BigDecimal totalScheduledInterest = installments.stream()
                .map(LoanRepaymentInstallment::getInterestDue).reduce(BigDecimal.ZERO, BigDecimal::add);
        loan.setOutstandingPrincipal(loan.getPrincipalAmount());
        loan.setOutstandingInterest(totalScheduledInterest);
        loan.setStatus(LoanStatus.ACTIVE);

        for (LoanTransaction txn : txns) {
            if (txn.isVoided() || txn.getTransactionType() != LoanTransactionType.REPAYMENT) {
                continue;
            }
            BigDecimal[] applied = applyRepaymentToInstallments(loan, txn.getAmount());
            loan.setOutstandingInterest(loan.getOutstandingInterest().subtract(applied[1]).max(BigDecimal.ZERO));
            loan.setOutstandingPrincipal(loan.getOutstandingPrincipal().subtract(applied[0]).max(BigDecimal.ZERO));
        }

        if (loan.getOutstandingPrincipal().compareTo(BigDecimal.ZERO) == 0
                && loan.getOutstandingInterest().compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus(LoanStatus.CLOSED);
        }
        loanAccountRepository.save(loan);
    }
}
