package com.destiny.club.repository;

import com.destiny.club.domain.loan.LoanTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LoanTransactionRepository extends JpaRepository<LoanTransaction, Long> {
    List<LoanTransaction> findByLoanAccountIdOrderByTransactionDateDescIdDesc(Long loanAccountId);
    List<LoanTransaction> findByTransactionDateBetweenOrderByTransactionDateAscIdAsc(LocalDate fromDate, LocalDate toDate);
}
