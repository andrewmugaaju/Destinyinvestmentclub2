package com.destiny.club.domain.loan;

import com.destiny.club.domain.client.Client;
import com.destiny.club.domain.client.Group;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loan_accounts")
@Getter
@Setter
@NoArgsConstructor
public class LoanAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String loanAccountNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_product_id", nullable = false)
    private LoanProduct loanProduct;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal principalAmount;

    @Column(nullable = false, precision = 6, scale = 3)
    private BigDecimal annualInterestRate;

    @Column(nullable = false)
    private Integer termMonths;

    /** Non-refundable fee charged when this loan is disbursed, captured at application time. */
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal applicationFeeAmount = BigDecimal.ZERO;

    private LocalDate applicationDate;
    private LocalDate disbursementDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status = LoanStatus.PENDING;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal outstandingPrincipal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal outstandingInterest = BigDecimal.ZERO;

    @Column(length = 60)
    private String approvedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "loanAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("installmentNumber ASC")
    private List<LoanRepaymentInstallment> installments = new ArrayList<>();

    public String getBorrowerName() {
        if (client != null) {
            return client.getFullName();
        }
        if (group != null) {
            return group.getGroupName() + " (group)";
        }
        return "-";
    }

    public BigDecimal getTotalOutstanding() {
        return outstandingPrincipal.add(outstandingInterest);
    }
}
