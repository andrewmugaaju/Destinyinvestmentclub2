package com.destiny.club.domain.accounting;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The header of a balanced double-entry transaction. Every posting to the general
 * ledger happens through a JournalEntry so that sum(debits) == sum(credits) always holds.
 */
@Entity
@Table(name = "journal_entries")
@Getter
@Setter
@NoArgsConstructor
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String reference;

    @Column(nullable = false)
    private LocalDate transactionDate;

    @Column(nullable = false, length = 250)
    private String description;

    /** The originating business transaction, e.g. SAVINGS_DEPOSIT, LOAN_REPAYMENT, MANUAL. */
    @Column(nullable = false, length = 40)
    private String sourceType;

    /** Id of the originating record (deposit transaction, loan transaction, etc.), if any. */
    private Long sourceId;

    @Column(length = 60)
    private String createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalEntryLine> lines = new ArrayList<>();

    public void addLine(JournalEntryLine line) {
        line.setJournalEntry(this);
        lines.add(line);
    }
}
