package com.destiny.club.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Backing object for the regular deposit screen: one total amount received from a member
 * (or group), split across one or more savings deposits and/or loan repayments. The split
 * lines must add up exactly to {@link #totalAmount}.
 */
public class DepositForm {

    private Long clientId;
    private Long groupId;
    private Long cashAccountId;
    private LocalDate transactionDate = LocalDate.now();
    private BigDecimal totalAmount;
    private String narration;
    private List<DepositAllocationForm> allocations = new ArrayList<>();

    public Long getCashAccountId() {
        return cashAccountId;
    }

    public void setCashAccountId(Long cashAccountId) {
        this.cashAccountId = cashAccountId;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getNarration() {
        return narration;
    }

    public void setNarration(String narration) {
        this.narration = narration;
    }

    public List<DepositAllocationForm> getAllocations() {
        return allocations;
    }

    public void setAllocations(List<DepositAllocationForm> allocations) {
        this.allocations = allocations;
    }
}
