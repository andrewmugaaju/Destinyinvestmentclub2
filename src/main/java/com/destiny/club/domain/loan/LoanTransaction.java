package com.destiny.club.domain.loan;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_transactions")
@Getter
@Setter
@NoArgsConstructor
public class LoanTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_account_id", nullable = false)
    private LoanAccount loanAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanTransactionType transactionType;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal principalPortion = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal interestPortion = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDate transactionDate;

    private Long journalEntryId;

    private Long depositTransactionId;

    @Column(length = 250)
    private String narration;

    @Column(length = 60)
    private String createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
