package com.destiny.club.repository;

import com.destiny.club.domain.loan.LoanAccount;
import com.destiny.club.domain.loan.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanAccountRepository extends JpaRepository<LoanAccount, Long> {
    boolean existsByLoanAccountNumber(String loanAccountNumber);
    List<LoanAccount> findByClientId(Long clientId);
    List<LoanAccount> findByGroupId(Long groupId);
    List<LoanAccount> findByStatus(LoanStatus status);
    List<LoanAccount> findByClientIdAndStatus(Long clientId, LoanStatus status);
    List<LoanAccount> findByGroupIdAndStatus(Long groupId, LoanStatus status);
}
