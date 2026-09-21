package com.destiny.club.repository;

import com.destiny.club.domain.deposit.DepositTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepositTransactionRepository extends JpaRepository<DepositTransaction, Long> {
    List<DepositTransaction> findAllByOrderByTransactionDateDescIdDesc();
}
