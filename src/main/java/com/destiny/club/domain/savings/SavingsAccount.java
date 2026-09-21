package com.destiny.club.domain.savings;

import com.destiny.club.domain.client.Client;
import com.destiny.club.domain.client.Group;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "savings_accounts")
@Getter
@Setter
@NoArgsConstructor
public class SavingsAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    /** Set when this is a group savings account rather than an individual one. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "savings_product_id", nullable = false)
    private SavingsProduct savingsProduct;

    @Column(nullable = false)
    private LocalDate openedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SavingsAccountStatus status = SavingsAccountStatus.ACTIVE;

    /** Cached running balance, kept in sync with the sum of savings transactions / GL postings. */
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public String getOwnerName() {
        if (client != null) {
            return client.getFullName();
        }
        if (group != null) {
            return group.getGroupName() + " (group)";
        }
        return "-";
    }
}
