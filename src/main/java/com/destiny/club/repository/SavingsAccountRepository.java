package com.destiny.club.repository;

import com.destiny.club.domain.savings.SavingsAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavingsAccountRepository extends JpaRepository<SavingsAccount, Long> {
    boolean existsByAccountNumber(String accountNumber);
    List<SavingsAccount> findByClientId(Long clientId);
    List<SavingsAccount> findByGroupId(Long groupId);
}
