package com.destiny.club.domain.accounting;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "journal_entry_lines")
@Getter
@Setter
@NoArgsConstructor
public class JournalEntryLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gl_account_id", nullable = false)
    private GLAccount glAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EntryType entryType;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    /** Optional reference to the client this line concerns, for subledger drill-down. */
    private Long clientId;

    @Column(length = 250)
    private String narration;

    public static JournalEntryLine debit(GLAccount account, BigDecimal amount, String narration) {
        JournalEntryLine line = new JournalEntryLine();
        line.setGlAccount(account);
        line.setEntryType(EntryType.DEBIT);
        line.setAmount(amount);
        line.setNarration(narration);
        return line;
    }

    public static JournalEntryLine credit(GLAccount account, BigDecimal amount, String narration) {
        JournalEntryLine line = new JournalEntryLine();
        line.setGlAccount(account);
        line.setEntryType(EntryType.CREDIT);
        line.setAmount(amount);
        line.setNarration(narration);
        return line;
    }
}
