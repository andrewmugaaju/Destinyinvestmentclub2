package com.destiny.club.repository;

import com.destiny.club.domain.loan.LoanProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanProductRepository extends JpaRepository<LoanProduct, Long> {
    boolean existsByCode(String code);
}
