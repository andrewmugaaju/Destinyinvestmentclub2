package com.destiny.club.service;

import com.destiny.club.domain.savings.SavingsProduct;
import com.destiny.club.exception.NotFoundException;
import com.destiny.club.repository.SavingsProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SavingsProductService {

    private final SavingsProductRepository savingsProductRepository;

    public List<SavingsProduct> findAll() {
        return savingsProductRepository.findAll();
    }

    public List<SavingsProduct> findActive() {
        return savingsProductRepository.findAll().stream().filter(SavingsProduct::isActive).toList();
    }

    public SavingsProduct getById(Long id) {
        return savingsProductRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Savings product not found: " + id));
    }

    public SavingsProduct save(SavingsProduct product) {
        return savingsProductRepository.save(product);
    }
}
