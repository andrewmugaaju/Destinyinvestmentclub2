package com.destiny.club.dto;

import com.destiny.club.domain.deposit.AllocationType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/** Read-only, human-friendly view of a deposit allocation for the deposit receipt page. */
@Getter
@AllArgsConstructor
public class DepositAllocationView {
    private final AllocationType allocationType;
    private final String accountNumber;
    private final String productName;
    private final BigDecimal amount;
}
