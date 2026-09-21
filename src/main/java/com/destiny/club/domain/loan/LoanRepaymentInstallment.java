package com.destiny.club.domain.loan;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loan_repayment_installments")
@Getter
@Setter
@NoArgsConstructor
public class LoanRepaymentInstallment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_account_id", nullable = false)
    private LoanAccount loanAccount;

    @Column(nullable = false)
    private Integer installmentNumber;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal principalDue;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal interestDue;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal principalPaid = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal interestPaid = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean fullyPaid = false;

    public BigDecimal getTotalDue() {
        return principalDue.add(interestDue);
    }

    public BigDecimal getPrincipalBalance() {
        return principalDue.subtract(principalPaid);
    }

    public BigDecimal getInterestBalance() {
        return interestDue.subtract(interestPaid);
    }

    public BigDecimal getBalance() {
        return getPrincipalBalance().add(getInterestBalance());
    }
}
