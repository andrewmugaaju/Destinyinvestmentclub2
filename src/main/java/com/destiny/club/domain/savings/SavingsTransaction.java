package com.destiny.club.domain.savings;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "savings_transactions")
@Getter
@Setter
@NoArgsConstructor
public class SavingsTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "savings_account_id", nullable = false)
    private SavingsAccount savingsAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SavingsTransactionType transactionType;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal runningBalance;

    @Column(nullable = false)
    private LocalDate transactionDate;

    /** Links back to the journal entry that recorded this transaction in the GL. */
    private Long journalEntryId;

    /** Links back to the deposit transaction screen entry, if this came from a combined deposit. */
    private Long depositTransactionId;

    @Column(length = 250)
    private String narration;

    @Column(length = 60)
    private String createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
