package com.destiny.club.repository;

import com.destiny.club.domain.accounting.JournalEntryLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface JournalEntryLineRepository extends JpaRepository<JournalEntryLine, Long> {

    /**
     * Per-account debit/credit totals for all postings up to and including {@code asOfDate}.
     * Used to build the trial balance and balance sheet.
     */
    @Query("select l.glAccount.id as accountId, l.entryType as entryType, sum(l.amount) as total " +
            "from JournalEntryLine l " +
            "where l.journalEntry.transactionDate <= :asOfDate " +
            "group by l.glAccount.id, l.entryType")
    List<AccountEntrySum> sumByAccountUpTo(@Param("asOfDate") LocalDate asOfDate);

    /**
     * Per-account debit/credit totals for postings within a date range (inclusive).
     * Used to build the income & expenditure statement for a period.
     */
    @Query("select l.glAccount.id as accountId, l.entryType as entryType, sum(l.amount) as total " +
            "from JournalEntryLine l " +
            "where l.journalEntry.transactionDate between :fromDate and :toDate " +
            "group by l.glAccount.id, l.entryType")
    List<AccountEntrySum> sumByAccountBetween(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Query("select l from JournalEntryLine l " +
            "where l.glAccount.id = :accountId " +
            "and (:fromDate is null or l.journalEntry.transactionDate >= :fromDate) " +
            "and l.journalEntry.transactionDate <= :toDate " +
            "order by l.journalEntry.transactionDate asc, l.journalEntry.id asc, l.id asc")
    List<JournalEntryLine> findLedgerLines(@Param("accountId") Long accountId,
                                            @Param("fromDate") LocalDate fromDate,
                                            @Param("toDate") LocalDate toDate);

    interface AccountEntrySum {
        Long getAccountId();
        com.destiny.club.domain.accounting.EntryType getEntryType();
        java.math.BigDecimal getTotal();
    }
}
