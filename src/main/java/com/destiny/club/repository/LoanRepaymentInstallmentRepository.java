package com.destiny.club.repository;

import com.destiny.club.domain.loan.LoanRepaymentInstallment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanRepaymentInstallmentRepository extends JpaRepository<LoanRepaymentInstallment, Long> {
    List<LoanRepaymentInstallment> findByLoanAccountIdOrderByInstallmentNumberAsc(Long loanAccountId);
}
