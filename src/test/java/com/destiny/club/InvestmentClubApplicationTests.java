package com.destiny.club;

import com.destiny.club.domain.accounting.GLAccount;
import com.destiny.club.domain.accounting.GLCodes;
import com.destiny.club.domain.client.Client;
import com.destiny.club.domain.client.Group;
import com.destiny.club.domain.loan.LoanAccount;
import com.destiny.club.domain.loan.LoanProduct;
import com.destiny.club.domain.savings.SavingsAccount;
import com.destiny.club.domain.savings.SavingsProduct;
import com.destiny.club.dto.DepositAllocationForm;
import com.destiny.club.dto.DepositForm;
import com.destiny.club.domain.deposit.AllocationType;
import com.destiny.club.dto.report.BalanceSheetReport;
import com.destiny.club.dto.report.TrialBalanceReport;
import com.destiny.club.repository.ClientRepository;
import com.destiny.club.repository.LoanRepaymentInstallmentRepository;
import com.destiny.club.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end smoke test against an in-memory H2 database (MySQL compatibility mode) that
 * exercises the full flow this application was built for: Flyway migrations + Hibernate
 * schema validation, opening a savings account and a loan, disbursing the loan, recording a
 * combined deposit that splits one receipt across both, and checking that the double-entry
 * reports (trial balance, balance sheet) stay in balance.
 */
@SpringBootTest
class InvestmentClubApplicationTests {

    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private GroupService groupService;
    @Autowired
    private ClientService clientService;
    @Autowired
    private SavingsProductService savingsProductService;
    @Autowired
    private LoanProductService loanProductService;
    @Autowired
    private SavingsService savingsService;
    @Autowired
    private LoanService loanService;
    @Autowired
    private DepositService depositService;
    @Autowired
    private AccountingService accountingService;
    @Autowired
    private LoanRepaymentInstallmentRepository loanRepaymentInstallmentRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void fullSavingsAndLoanDepositFlowKeepsBooksBalanced() {
        Client client = new Client();
        client.setFirstName("Jane");
        client.setLastName("Mwangi");
        client.setPhone("0712345678");
        client = clientService.save(client, null);

        SavingsProduct savingsProduct = new SavingsProduct();
        savingsProduct.setCode("SP-TEST");
        savingsProduct.setName("Regular Savings");
        savingsProduct.setAnnualInterestRate(new BigDecimal("5.000"));
        savingsProduct.setMinOpeningBalance(BigDecimal.ZERO);
        savingsProduct = savingsProductService.save(savingsProduct);

        LoanProduct loanProduct = new LoanProduct();
        loanProduct.setCode("LP-TEST");
        loanProduct.setName("Business Loan");
        loanProduct.setAnnualInterestRate(new BigDecimal("12.000"));
        loanProduct.setDefaultTermMonths(6);
        loanProduct = loanProductService.save(loanProduct);

        SavingsAccount savingsAccount = savingsService.openAccount(client, null, savingsProduct, LocalDate.now());
        GLAccount cashAccount = accountingService.getAccountByCode(GLCodes.CASH_AND_BANK);

        LoanAccount loan = loanService.apply(client, null, loanProduct, new BigDecimal("1000.00"), 6, new BigDecimal("20.00"));
        loan = loanService.approve(loan.getId(), "tester");
        loan = loanService.disburse(loan.getId(), LocalDate.now(), "tester", cashAccount);
        assertThat(loanRepaymentInstallmentRepository.findByLoanAccountIdOrderByInstallmentNumberAsc(loan.getId())).hasSize(6);
        // Outstanding right after disbursement covers the principal plus the full scheduled interest for the term.
        BigDecimal outstandingBeforeRepayment = loan.getTotalOutstanding();

        // Combined deposit: 300 total = 100 to savings + 200 towards the loan repayment.
        DepositForm form = new DepositForm();
        form.setClientId(client.getId());
        form.setCashAccountId(cashAccount.getId());
        form.setTransactionDate(LocalDate.now());
        form.setTotalAmount(new BigDecimal("300.00"));

        DepositAllocationForm savingsAlloc = new DepositAllocationForm();
        savingsAlloc.setAllocationType(AllocationType.SAVINGS_DEPOSIT);
        savingsAlloc.setTargetAccountId(savingsAccount.getId());
        savingsAlloc.setAmount(new BigDecimal("100.00"));

        DepositAllocationForm loanAlloc = new DepositAllocationForm();
        loanAlloc.setAllocationType(AllocationType.LOAN_REPAYMENT);
        loanAlloc.setTargetAccountId(loan.getId());
        loanAlloc.setAmount(new BigDecimal("200.00"));

        form.setAllocations(List.of(savingsAlloc, loanAlloc));

        var deposit = depositService.record(form, "tester");
        assertThat(deposit.getJournalEntryId()).isNotNull();

        SavingsAccount reloadedSavings = savingsService.getById(savingsAccount.getId());
        assertThat(reloadedSavings.getBalance()).isEqualByComparingTo("100.00");

        LoanAccount reloadedLoan = loanService.getById(loan.getId());
        assertThat(reloadedLoan.getTotalOutstanding()).isEqualByComparingTo(
                outstandingBeforeRepayment.subtract(new BigDecimal("200.00")));

        TrialBalanceReport trialBalance = accountingService.generateTrialBalance(LocalDate.now());
        assertThat(trialBalance.isBalanced()).isTrue();

        BalanceSheetReport balanceSheet = accountingService.generateBalanceSheet(LocalDate.now());
        assertThat(balanceSheet.isBalanced()).isTrue();
    }

    @Test
    void groupCanBeCreatedAndReferencedByClient() {
        Group group = new Group();
        group.setGroupName("Umoja Group");
        group = groupService.save(group);
        assertThat(group.getGroupNumber()).isNotNull();

        Client client = new Client();
        client.setFirstName("John");
        client.setLastName("Otieno");
        client = clientService.save(client, group.getId());

        assertThat(clientRepository.findById(client.getId()).orElseThrow().getGroup().getId()).isEqualTo(group.getId());
    }
}
