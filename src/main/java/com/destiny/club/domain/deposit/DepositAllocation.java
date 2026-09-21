package com.destiny.club.domain.deposit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "deposit_allocations")
@Getter
@Setter
@NoArgsConstructor
public class DepositAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deposit_transaction_id", nullable = false)
    private DepositTransaction depositTransaction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AllocationType allocationType;

    /** Id of the SavingsAccount or LoanAccount this allocation is posted against. */
    @Column(nullable = false)
    private Long targetAccountId;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;
}
