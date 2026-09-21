package com.destiny.club.repository;

import com.destiny.club.domain.accounting.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    @Query("select je from JournalEntry je order by je.transactionDate desc, je.id desc")
    List<JournalEntry> findRecent();
}
