package com.destiny.club.domain.deposit;

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

/**
 * The "regular deposit" screen: a teller records ONE total amount received from a
 * member (or group), then splits it across one or more savings deposits and/or loan
 * repayments. The sum of the allocations must equal the total amount captured.
 */
@Entity
@Table(name = "deposit_transactions")
@Getter
@Setter
@NoArgsConstructor
public class DepositTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @Column(nullable = false)
    private LocalDate transactionDate;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    private Long journalEntryId;

    @Column(length = 250)
    private String narration;

    @Column(length = 60)
    private String createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "depositTransaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DepositAllocation> allocations = new ArrayList<>();

    public void addAllocation(DepositAllocation allocation) {
        allocation.setDepositTransaction(this);
        allocations.add(allocation);
    }
}
