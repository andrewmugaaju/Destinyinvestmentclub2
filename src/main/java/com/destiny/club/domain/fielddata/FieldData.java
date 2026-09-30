package com.destiny.club.domain.fielddata;

import com.destiny.club.domain.client.Client;
import com.destiny.club.domain.savings.SavingsTransaction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One cash collection made out in the field: a field officer (working that day in a rotating
 * pair, not tied to the member's own permanent group) collects a savings deposit from a member.
 * The actual money movement - the savings deposit itself, its journal entry, its running balance
 * - all lives on the linked {@link SavingsTransaction}; this record exists purely to capture who
 * physically did the collection.
 */
@Entity
@Table(name = "field_data")
@Getter
@Setter
@NoArgsConstructor
public class FieldData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    /** The savings deposit this collection created - carries the amount, date, narration, journal entry and void state. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "savings_transaction_id", nullable = false, unique = true)
    private SavingsTransaction savingsTransaction;

    /** The two field officers change daily/weekly and are recorded here as plain names, not system users. */
    @Column(nullable = false, length = 120)
    private String collectorOneName;

    @Column(length = 120)
    private String collectorTwoName;

    @Column(length = 60)
    private String createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
