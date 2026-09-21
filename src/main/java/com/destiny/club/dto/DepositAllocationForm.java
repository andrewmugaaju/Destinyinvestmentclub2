package com.destiny.club.dto;

import com.destiny.club.domain.deposit.AllocationType;

import java.math.BigDecimal;

public class DepositAllocationForm {

    private AllocationType allocationType;
    private Long targetAccountId;
    private BigDecimal amount;

    public AllocationType getAllocationType() {
        return allocationType;
    }

    public void setAllocationType(AllocationType allocationType) {
        this.allocationType = allocationType;
    }

    public Long getTargetAccountId() {
        return targetAccountId;
    }

    public void setTargetAccountId(Long targetAccountId) {
        this.targetAccountId = targetAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
