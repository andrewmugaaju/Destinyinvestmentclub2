package com.destiny.club.repository;

import com.destiny.club.domain.accounting.AccountType;
import com.destiny.club.domain.accounting.GLAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GLAccountRepository extends JpaRepository<GLAccount, Long> {
    Optional<GLAccount> findByCode(String code);
    List<GLAccount> findByAccountTypeOrderByCode(AccountType accountType);
    List<GLAccount> findAllByOrderByCode();
}
