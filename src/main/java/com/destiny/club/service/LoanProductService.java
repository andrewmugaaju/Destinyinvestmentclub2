package com.destiny.club.service;

import com.destiny.club.domain.loan.LoanProduct;
import com.destiny.club.exception.NotFoundException;
import com.destiny.club.repository.LoanProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanProductService {

    private final LoanProductRepository loanProductRepository;

    public List<LoanProduct> findAll() {
        return loanProductRepository.findAll();
    }

    public List<LoanProduct> findActive() {
        return loanProductRepository.findAll().stream().filter(LoanProduct::isActive).toList();
    }

    public LoanProduct getById(Long id) {
        return loanProductRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Loan product not found: " + id));
    }

    public LoanProduct save(LoanProduct product) {
        return loanProductRepository.save(product);
    }
}
