package com.destiny.club.repository;

import com.destiny.club.domain.savings.SavingsProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavingsProductRepository extends JpaRepository<SavingsProduct, Long> {
    boolean existsByCode(String code);
}
