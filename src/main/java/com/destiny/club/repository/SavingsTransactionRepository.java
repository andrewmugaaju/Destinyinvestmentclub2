package com.destiny.club.repository;

import com.destiny.club.domain.savings.SavingsTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavingsTransactionRepository extends JpaRepository<SavingsTransaction, Long> {
    List<SavingsTransaction> findBySavingsAccountIdOrderByTransactionDateDescIdDesc(Long savingsAccountId);
}
