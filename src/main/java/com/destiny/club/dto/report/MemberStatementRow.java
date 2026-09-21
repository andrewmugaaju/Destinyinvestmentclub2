package com.destiny.club.dto.report;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class MemberStatementRow {
    private final LocalDate date;
    private final String description;
    private final String reference;
    private final String accountNumber;
    /** Positive for a savings deposit, negative for a withdrawal, null if this row is a loan movement. */
    private final BigDecimal savingsAmount;
    private final BigDecimal savingsBalance;
    /** Positive for a disbursement, negative for a repayment, null if this row is a savings movement. */
    private final BigDecimal loanAmount;
    private final BigDecimal loanBalance;
}
