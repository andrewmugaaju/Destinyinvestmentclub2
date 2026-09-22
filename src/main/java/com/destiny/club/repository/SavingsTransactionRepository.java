package com.destiny.club.repository;

import com.destiny.club.domain.savings.SavingsTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SavingsTransactionRepository extends JpaRepository<SavingsTransaction, Long> {
    List<SavingsTransaction> findBySavingsAccountIdOrderByTransactionDateDescIdDesc(Long savingsAccountId);
    List<SavingsTransaction> findByTransactionDateBetweenOrderByTransactionDateAscIdAsc(LocalDate fromDate, LocalDate toDate);
}
