package com.destiny.club.service;

import com.destiny.club.domain.accounting.GLAccount;
import com.destiny.club.domain.accounting.GLCodes;
import com.destiny.club.domain.accounting.JournalEntry;
import com.destiny.club.domain.accounting.JournalEntryLine;
import com.destiny.club.domain.client.Client;
import com.destiny.club.domain.client.Group;
import com.destiny.club.domain.deposit.AllocationType;
import com.destiny.club.domain.deposit.DepositAllocation;
import com.destiny.club.domain.deposit.DepositTransaction;
import com.destiny.club.domain.loan.LoanAccount;
import com.destiny.club.domain.loan.LoanTransaction;
import com.destiny.club.domain.savings.SavingsAccount;
import com.destiny.club.domain.savings.SavingsTransaction;
import com.destiny.club.dto.DepositAllocationForm;
import com.destiny.club.dto.DepositForm;
import com.destiny.club.exception.BusinessException;
import com.destiny.club.exception.NotFoundException;
import com.destiny.club.repository.ClientRepository;
import com.destiny.club.repository.DepositTransactionRepository;
import com.destiny.club.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Backs the "regular deposit" screen: a teller captures one total amount received from a
 * member or group and splits it across one or more savings deposits and/or loan repayments.
 * The split must add up exactly to the total, and the whole receipt is posted as a single
 * balanced journal entry.
 */
@Service
@RequiredArgsConstructor
public class DepositService {

    private final DepositTransactionRepository depositTransactionRepository;
    private final ClientRepository clientRepository;
    private final GroupRepository groupRepository;
    private final SavingsService savingsService;
    private final LoanService loanService;
    private final AccountingService accountingService;

    private static final DateTimeFormatter REF_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public List<DepositTransaction> findAll() {
        return depositTransactionRepository.findAllByOrderByTransactionDateDescIdDesc();
    }

    public DepositTransaction getById(Long id) {
        return depositTransactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Deposit transaction not found: " + id));
    }

    @Transactional
    public DepositTransaction record(DepositForm form, String createdBy) {
        if (form.getTotalAmount() == null || form.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Total deposit amount must be greater than zero");
        }
        if (form.getAllocations() == null || form.getAllocations().isEmpty()) {
            throw new BusinessException("At least one savings or loan repayment split is required");
        }
        if (form.getClientId() == null && form.getGroupId() == null) {
            throw new BusinessException("A client or a group must be selected for this deposit");
        }

        BigDecimal splitTotal = form.getAllocations().stream()
                .map(a -> a.getAmount() != null ? a.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (splitTotal.compareTo(form.getTotalAmount()) != 0) {
            throw new BusinessException("The split (" + splitTotal + ") must add up exactly to the total deposit amount (" + form.getTotalAmount() + ")");
        }

        Client client = null;
        Group group = null;
        if (form.getClientId() != null) {
            client = clientRepository.findById(form.getClientId())
                    .orElseThrow(() -> new NotFoundException("Client not found: " + form.getClientId()));
        }
        if (form.getGroupId() != null) {
            group = groupRepository.findById(form.getGroupId())
                    .orElseThrow(() -> new NotFoundException("Group not found: " + form.getGroupId()));
        }

        LocalDate date = form.getTransactionDate() != null ? form.getTransactionDate() : LocalDate.now();

        DepositTransaction deposit = new DepositTransaction();
        deposit.setClient(client);
        deposit.setGroup(group);
        deposit.setTransactionDate(date);
        deposit.setTotalAmount(form.getTotalAmount());
        deposit.setNarration(form.getNarration());
        deposit.setCreatedBy(createdBy);
        deposit.setReference("DEP-" + date.format(REF_FORMAT) + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());

        BigDecimal savingsTotal = BigDecimal.ZERO;
        BigDecimal loanPrincipalTotal = BigDecimal.ZERO;
        BigDecimal loanInterestTotal = BigDecimal.ZERO;

        List<SavingsTransaction> savingsTxns = new ArrayList<>();
        List<LoanTransaction> loanTxns = new ArrayList<>();

        for (DepositAllocationForm allocationForm : form.getAllocations()) {
            if (allocationForm.getAmount() == null || allocationForm.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            DepositAllocation allocation = new DepositAllocation();
            allocation.setAllocationType(allocationForm.getAllocationType());
            allocation.setTargetAccountId(allocationForm.getTargetAccountId());
            allocation.setAmount(allocationForm.getAmount());
            deposit.addAllocation(allocation);

            if (allocationForm.getAllocationType() == AllocationType.SAVINGS_DEPOSIT) {
                SavingsAccount account = savingsService.getById(allocationForm.getTargetAccountId());
                SavingsTransaction txn = savingsService.recordDeposit(account, allocationForm.getAmount(), date,
                        "Deposit " + deposit.getReference(), createdBy);
                txn.setDepositTransactionId(null); // set after deposit has an id, below
                savingsTxns.add(txn);
                savingsTotal = savingsTotal.add(allocationForm.getAmount());
            } else {
                LoanAccount loan = loanService.getById(allocationForm.getTargetAccountId());
                LoanTransaction txn = loanService.recordRepayment(loan, allocationForm.getAmount(), date,
                        "Repayment " + deposit.getReference(), createdBy);
                txn.setDepositTransactionId(null);
                loanTxns.add(txn);
                loanPrincipalTotal = loanPrincipalTotal.add(txn.getPrincipalPortion());
                loanInterestTotal = loanInterestTotal.add(txn.getInterestPortion());
            }
        }

        deposit = depositTransactionRepository.save(deposit);

        for (SavingsTransaction txn : savingsTxns) {
            txn.setDepositTransactionId(deposit.getId());
        }
        for (LoanTransaction txn : loanTxns) {
            txn.setDepositTransactionId(deposit.getId());
        }

        List<JournalEntryLine> lines = new ArrayList<>();
        GLAccount cash = accountingService.getAccountByCode(GLCodes.CASH_AND_BANK);
        lines.add(JournalEntryLine.debit(cash, form.getTotalAmount(), "Cash received - " + deposit.getReference()));

        if (savingsTotal.compareTo(BigDecimal.ZERO) > 0) {
            GLAccount savingsControl = accountingService.getAccountByCode(GLCodes.MEMBER_SAVINGS);
            lines.add(JournalEntryLine.credit(savingsControl, savingsTotal, "Savings deposits - " + deposit.getReference()));
        }
        if (loanPrincipalTotal.compareTo(BigDecimal.ZERO) > 0) {
            GLAccount loansReceivable = accountingService.getAccountByCode(GLCodes.LOANS_RECEIVABLE);
            lines.add(JournalEntryLine.credit(loansReceivable, loanPrincipalTotal, "Loan principal repayments - " + deposit.getReference()));
        }
        if (loanInterestTotal.compareTo(BigDecimal.ZERO) > 0) {
            GLAccount interestIncome = accountingService.getAccountByCode(GLCodes.LOAN_INTEREST_INCOME);
            lines.add(JournalEntryLine.credit(interestIncome, loanInterestTotal, "Loan interest repayments - " + deposit.getReference()));
        }

        String description = "Deposit " + deposit.getReference()
                + (client != null ? " - " + client.getFullName() : "")
                + (group != null ? " - " + group.getGroupName() : "");

        JournalEntry entry = accountingService.post(date, "DEPOSIT_TRANSACTION", deposit.getId(), description, createdBy, lines);
        deposit.setJournalEntryId(entry.getId());

        for (SavingsTransaction txn : savingsTxns) {
            txn.setJournalEntryId(entry.getId());
        }
        for (LoanTransaction txn : loanTxns) {
            txn.setJournalEntryId(entry.getId());
        }

        return depositTransactionRepository.save(deposit);
    }
}
